const fs = require('fs');
const crypto = require('crypto');

module.exports = async ({github, context, core}) => {
  const {owner, repo} = context.repo;
  const notes = fs.readFileSync('RELEASE_NOTES.md', 'utf8');
  const revision = `\n\n[Исходники донатерской сборки](https://github.com/${owner}/${repo}/tree/${context.sha}/versions). Версия JAR: **2.1.0**.`;
  const targets = [
    {tag:'v2.0.0', id:461075957, latest:'true', files:['RuleHelper-26.3-Fabric.jar','RuleHelper-1.21.11-Fabric.jar','RuleHelper-1.21.4-Fabric.jar']},
    {tag:'v1.3.3-26.3', id:459849450, latest:'false', files:['RuleHelper-26.3-Fabric.jar']},
    {tag:'v1.2.1', id:422976338, latest:'false', files:['RuleHelper-1.21.11-Fabric.jar','RuleHelper-1.21.4-Fabric.jar']},
  ];
  for (const target of targets) {
    const release = (await github.rest.repos.getReleaseByTag({owner, repo, tag:target.tag})).data;
    if (release.id !== target.id || release.draft) throw new Error('Unexpected release identity: '+target.tag);
    let assets = await github.paginate(github.rest.repos.listReleaseAssets, {owner, repo, release_id:release.id, per_page:100});
    const staged = [];
    for (const name of target.files) {
      const data = fs.readFileSync(`release-assets/${name}`);
      const digest = 'sha256:'+crypto.createHash('sha256').update(data).digest('hex');
      const temporaryName = `${name}.replacement-${context.sha.substring(0,12)}`;
      let asset = assets.find(a => a.name === temporaryName);
      if (!asset) asset = (await github.rest.repos.uploadReleaseAsset({
        owner, repo, release_id:release.id, name:temporaryName, data,
        headers:{'content-type':'application/java-archive','content-length':data.length},
      })).data;
      if (asset.state !== 'uploaded' || asset.size !== data.length || (asset.digest && asset.digest !== digest)) {
        throw new Error('Staged upload failed validation: '+name);
      }
      staged.push({name, asset});
    }
    // Each replacement is uploaded and checked before its previous download is removed.
    for (const {name, asset} of staged) {
      for (const previous of assets.filter(a => a.name === name)) {
        await github.rest.repos.deleteReleaseAsset({owner, repo, asset_id:previous.id});
      }
      await github.rest.repos.updateReleaseAsset({owner, repo, asset_id:asset.id, name, label:''});
      core.info('Replaced '+target.tag+'/'+name);
    }
    await github.rest.repos.updateRelease({owner, repo, release_id:release.id,
      body:notes+revision, draft:false, prerelease:false, make_latest:target.latest});
  }
};
