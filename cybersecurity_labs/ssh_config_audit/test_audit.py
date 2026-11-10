import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from audit import audit, parse_snapshot, RULES


GOOD = '\n'.join(f'{r.key} {"3" if r.key=="maxauthtries" else "30" if r.key=="logingracetime" else r.expected}' for r in RULES)


class AuditTests(unittest.TestCase):
    def test_complete_baseline(self):
        report=audit(GOOD)
        self.assertEqual(report['verdict'], 'matches_baseline')
        self.assertEqual(report['counts'], {'pass':10,'review':0,'unknown':0})

    def test_missing_values_are_unknown(self):
        r=audit('permitrootlogin no')
        self.assertEqual(r['counts']['unknown'], 9)
        self.assertEqual(r['verdict'], 'review_required')

    def test_different_root_policy_requires_review(self):
        r=audit(GOOD.replace('permitrootlogin no','permitrootlogin prohibit-password'))
        self.assertEqual(r['counts']['review'],1)

    def test_numeric_boundaries(self):
        for n,expected in [('0','review'),('1','pass'),('4','pass'),('5','review'),('-1','review'),('3x','review')]:
            with self.subTest(n=n):
                r=audit(GOOD.replace('maxauthtries 3',f'maxauthtries {n}'))
                self.assertEqual(next(f['status'] for f in r['findings'] if f['setting']=='maxauthtries'),expected)

    def test_grace_time_boundaries(self):
        for n,expected in [('0','review'),('60','pass'),('61','review'),('1m','review')]:
            with self.subTest(n=n):
                r=audit(GOOD.replace('logingracetime 30',f'logingracetime {n}'))
                self.assertEqual(next(f['status'] for f in r['findings'] if f['setting']=='logingracetime'),expected)

    def test_match_and_include_rejected(self):
        for directive in ['Match User student', 'Include /etc/ssh/sshd_config.d/*.conf']:
            with self.assertRaises(ValueError): audit(GOOD+'\n'+directive)

    def test_duplicate_checked_setting_rejected(self):
        with self.assertRaises(ValueError): audit(GOOD+'\nPermitRootLogin yes')

    def test_unchecked_multi_value_settings_allowed(self):
        r=audit(GOOD+'\nlistenaddress [::]:22\nlistenaddress 0.0.0.0:22')
        self.assertEqual(r['verdict'],'matches_baseline')

    def test_keyword_case_and_whitespace(self):
        self.assertEqual(parse_snapshot('  PermitRootLogin   no  \n# note'),{'permitrootlogin':'no'})

    def test_invalid_or_empty_snapshot(self):
        for text in ['', '# empty', 'port 22','permitrootlogin']:
            with self.subTest(text=text), self.assertRaises(ValueError): audit(text)

    def test_input_size_guard(self):
        with self.assertRaises(ValueError): audit('x'*2_000_001)

    def test_cli_exit_and_json(self):
        with tempfile.TemporaryDirectory() as directory:
            p=Path(directory)/'snapshot.txt'; p.write_text('permitrootlogin yes')
            r=subprocess.run([sys.executable,str(Path(__file__).with_name('audit.py')),str(p),'--fail-on-review'],text=True,capture_output=True)
            self.assertEqual(r.returncode,1)
            self.assertEqual(json.loads(r.stdout)['counts']['review'],1)


if __name__ == '__main__': unittest.main()
