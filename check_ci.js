#!/usr/bin/env node
// 查询 Actions run 的 job 步骤状态
const https = require('https');
const runId = process.argv[2];
const token = process.argv[3];
const url = `https://api.github.com/repos/xis3794/OllamaServer/actions/runs/${runId}/jobs`;
https.get(url, { headers: { 'Authorization': 'token ' + token, 'User-Agent': 'node', 'Accept': 'application/vnd.github+json' } }, (res) => {
  let d = '';
  res.on('data', c => d += c);
  res.on('end', () => {
    try {
      const j = JSON.parse(d);
      if (!j.jobs || j.jobs.length === 0) { console.log('no job yet'); return; }
      j.jobs.forEach(job => {
        console.log('job:', job.name, '| status:', job.status);
        (job.steps || []).forEach(s => console.log('  ', s.number, s.name, '->', s.status, s.conclusion || ''));
      });
    } catch (e) { console.log('parse error:', d.slice(0, 300)); }
  });
}).on('error', e => console.log('req error:', e.message));