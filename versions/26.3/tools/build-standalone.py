#!/usr/bin/env python3
"""Build the unobfuscated Minecraft 26.3 client mod using JDK 25 and official jars.
Useful where Gradle/Loom's local socket feature detection is unavailable.
Minecraft and library jars are cached locally and are never bundled with the mod.
"""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import hashlib, json, os, shutil, subprocess, urllib.request

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / '.standalone-cache'
CACHE.mkdir(exist_ok=True)

def get(url, destination, expected_sha1=None):
    if destination.exists() and (not expected_sha1 or hashlib.sha1(destination.read_bytes()).hexdigest() == expected_sha1):
        return destination
    data = urllib.request.urlopen(url, timeout=120).read()
    if expected_sha1 and hashlib.sha1(data).hexdigest() != expected_sha1:
        raise RuntimeError('SHA-1 mismatch for ' + url)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    return destination

def tool(name):
    base = os.environ.get('JAVA_HOME')
    result = Path(base) / 'bin' / (name + ('.exe' if os.name == 'nt' else '')) if base else shutil.which(name)
    if not result:
        raise RuntimeError('Install JDK 25 and set JAVA_HOME')
    return str(result)

manifest = json.loads(urllib.request.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json', timeout=60).read())
version_url = next(v['url'] for v in manifest['versions'] if v['id'] == '26.3')
version = json.loads(urllib.request.urlopen(version_url, timeout=60).read())
client = version['downloads']['client']
client_jar = get(client['url'], CACHE / 'minecraft-26.3-client.jar', client['sha1'])
artifacts = [v['downloads']['artifact'] for v in version['libraries'] if v.get('downloads', {}).get('artifact') and 'natives-' not in v['downloads']['artifact']['path']]
with ThreadPoolExecutor(max_workers=8) as pool:
    libs = list(pool.map(lambda a: get(a['url'], CACHE / 'libs' / Path(a['path']).name, a.get('sha1')), artifacts))
for url in [
    'https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.20.1/fabric-loader-0.20.1.jar',
    'https://maven.fabricmc.net/net/fabricmc/sponge-mixin/0.17.0+mixin.0.8.7/sponge-mixin-0.17.0+mixin.0.8.7.jar',
    *[f'https://maven.fabricmc.net/org/ow2/asm/{a}/9.10.1/{a}-9.10.1.jar' for a in ['asm', 'asm-tree', 'asm-commons', 'asm-util', 'asm-analysis']],
]:
    libs.append(get(url, CACHE / 'libs' / url.rsplit('/', 1)[1]))
classes = ROOT / 'build/classes/java/main'
tests = ROOT / 'build/classes/java/test'
shutil.rmtree(classes, ignore_errors=True)
classes.mkdir(parents=True)
tests.mkdir(parents=True, exist_ok=True)
cp = os.pathsep.join(str(p) for p in [client_jar, *libs])
subprocess.run([tool('javac'), '--release', '25', '-encoding', 'UTF-8', '-cp', cp, '-processorpath', cp, '-processor', 'org.spongepowered.tools.obfuscation.MixinObfuscationProcessorInjection,org.spongepowered.tools.obfuscation.MixinObfuscationProcessorTargets', '-d', str(classes), *map(str, sorted((ROOT / 'src/main/java').rglob('*.java')))], check=True)
subprocess.run([tool('javac'), '--release', '25', '-encoding', 'UTF-8', '-cp', str(classes), '-d', str(tests), *map(str, sorted((ROOT / 'src/test/java').rglob('*.java')))], check=True)
subprocess.run([tool('java'), '-cp', os.pathsep.join([str(classes), str(tests)]), 'ru.dmitry.rulehelper.chat.PortRegressionTest'], check=True)
output = ROOT / 'build/libs/RuleHelper-26.3-Fabric-TEST.jar'
output.parent.mkdir(parents=True, exist_ok=True)
subprocess.run([tool('jar'), '--create', '--file', str(output), '-C', str(classes), '.', '-C', str(ROOT / 'src/main/resources'), '.'], check=True)
print(output)
print('SHA-256:', hashlib.sha256(output.read_bytes()).hexdigest())
