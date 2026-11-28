"""Verify release identity and donor boundaries before a JAR can be published."""
import json
import sys
from pathlib import Path
from zipfile import ZipFile

path, minecraft = Path(sys.argv[1]), sys.argv[2]
with ZipFile(path) as jar:
    assert jar.testzip() is None, 'Corrupt JAR'
    metadata = json.loads(jar.read('fabric.mod.json'))
    assert metadata['id'] == 'rule_helper', metadata
    assert metadata['version'] == '2.1.0', metadata
    assert metadata['depends']['minecraft'] == '~' + minecraft, metadata
    names = jar.namelist()
    required = ['ui/CustomReasonScreen.class', 'ui/DonatorSettingsScreen.class',
                'ui/DailyPunishmentHistoryScreen.class', 'PunishmentCorrectionCommands.class', 'ReasonTemplates.class',
                'chat/PrivateMessageMenu.class', 'chat/CoreProtectNameCopy.class',
                'ui/NotificationSettingsScreen.class', 'PunishmentRemovalCommands.class',
                'ui/PunishmentRemovalScreen.class']
    for suffix in required:
        assert 'ru/dmitry/rulehelper/' + suffix in names, 'Missing ' + suffix
    forbidden = ['ProofRequest', 'ModeratorProof', 'ModerationSettings', 'ViolationHistory',
                 'ViolationCountScreen', 'ProofFollow', 'OffenderQueue', 'PunishmentPresetScreen',
                 'EvidenceRequest', 'PunishmentScopeFormatter']
    leaked = [name for name in names if any(token in name for token in forbidden)]
    assert not leaked, 'Moderator-only classes found: ' + repr(leaked)
print(f'PASS: donor release {metadata["version"]}, Minecraft {minecraft}, {path.stat().st_size} bytes')
