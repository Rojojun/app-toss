"""Exercise replacement and failure recovery without Docker or a real database."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name('deploy-home.sh').resolve()
FAKE_DOCKER = r'''#!/usr/bin/env python3
import json, os, sys
from pathlib import Path
p=Path(os.environ['TEST_STATE'])
s=json.loads(p.read_text())
a=sys.argv[1:]
s['calls'].append(a)
code=0
if a[0]=='inspect':
 name=a[-1]
 if name not in s['containers']: code=1
 elif '.State.Running' in a[2]: print(str(s['containers'][name]['running']).lower())
 else: print('connected')
elif a[:2]==['container','inspect']:
 code=0 if a[-1] in s['containers'] else 1
elif a[:2]==['network','inspect']: pass
elif a[0]=='create':
 name=a[a.index('--name')+1]
 s['containers'][name]={'running':False, 'image':a[-1]}
 print('test-container')
elif a[0]=='stop': s['containers'][a[-1]]['running']=False
elif a[0]=='rename': s['containers'][a[2]]=s['containers'].pop(a[1])
elif a[0]=='start': s['containers'][a[-1]]['running']=True
elif a[0]=='rm': s['containers'].pop(a[-1],None)
elif a[0]=='logs': print('simulated application log')
elif a[0]=='pull' or a[:2]==['image','inspect']: pass
else: raise SystemExit('Unexpected fake Docker operation')
p.write_text(json.dumps(s))
sys.exit(code)
'''

class DeployTest(unittest.TestCase):
    def deploy(self, previous=False, fail_health=False, invalid_env=False):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            bin_dir=root/'bin'
            bin_dir.mkdir()
            commands={
                'docker':FAKE_DOCKER,
                'curl':'#!/bin/sh\nif [ "$TEST_FAIL_HEALTH" = 1 ]; then exit 22; fi\nprintf \'{"status":"UP"}\'\n',
                'flock':'#!/bin/sh\nexit 0\n',
                'sleep':'#!/bin/sh\nexit 0\n',
            }
            for name,content in commands.items():
                p=bin_dir/name
                p.write_text(content)
                p.chmod(0o700)
            certs=root/'certs'
            certs.mkdir()
            for name in ('toss.crt','toss.key'):
                (certs/name).write_text('test fixture')
            env_file=root/'.env'
            env_file.write_text('DB_URL=jdbc:'+('mysql' if invalid_env else 'postgresql')+'://postgres:5432/test\n')
            containers={'postgres':{'running':True,'image':'postgres:17'}}
            if previous:
                containers['houselink-app']={'running':True,'image':'old-image'}
            state=root/'state.json'
            state.write_text(json.dumps({'containers':containers,'calls':[]}))
            env={**os.environ,'PATH':str(bin_dir)+os.pathsep+os.environ['PATH'],
                 'APP_ENV_FILE':str(env_file),'APP_CERTS_DIR':str(certs),
                 'APP_STATE_DIR':str(root/'locks'),'TEST_STATE':str(state),
                 'TEST_FAIL_HEALTH':'1' if fail_health else '0','APP_PULL':'false'}
            result=subprocess.run(['bash',str(SCRIPT),'new-image'],env=env,capture_output=True,text=True)
            return result,json.loads(state.read_text())

    def test_first_deployment(self):
        result,state=self.deploy()
        self.assertEqual(result.returncode,0,result.stderr)
        self.assertEqual(state['containers']['houselink-app']['image'],'new-image')
        self.assertNotIn('houselink-app-next',state['containers'])
        self.assertFalse(any(call[0]=='pull' for call in state['calls']))

    def test_successful_replacement_removes_previous_container(self):
        result,state=self.deploy(previous=True)
        self.assertEqual(result.returncode,0,result.stderr)
        self.assertEqual(set(state['containers']),{'postgres','houselink-app'})
        self.assertEqual(state['containers']['houselink-app']['image'],'new-image')

    def test_health_failure_restores_previous_app_without_touching_db(self):
        result,state=self.deploy(previous=True,fail_health=True)
        self.assertNotEqual(result.returncode,0)
        self.assertEqual(state['containers']['houselink-app'],{'running':True,'image':'old-image'})
        self.assertEqual(state['containers']['postgres'],{'running':True,'image':'postgres:17'})
        self.assertEqual(set(state['containers']),{'postgres','houselink-app'})

    def test_invalid_config_leaves_previous_app_running(self):
        result,state=self.deploy(previous=True,invalid_env=True)
        self.assertNotEqual(result.returncode,0)
        self.assertTrue(state['containers']['houselink-app']['running'])
        self.assertFalse(any(call[0]=='stop' for call in state['calls']))

if __name__=='__main__':
    unittest.main()
