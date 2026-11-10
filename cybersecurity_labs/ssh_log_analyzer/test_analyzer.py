import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from analyzer import analyze


def line(second=0, outcome='Failed', ip='192.0.2.10', user='student', host='lab', minute=0):
    return f'2026-11-10T09:{minute:02}:{second:02}+00:00 {host} sshd[42]: {outcome} password for {user} from {ip} port 2222 ssh2'


class AnalyzerTests(unittest.TestCase):
    def test_burst_and_following_success(self):
        r = analyze([line(i) for i in range(5)] + [line(6, 'Accepted')])
        self.assertEqual([a['type'] for a in r['alerts']], ['failure_burst', 'success_after_failures'])
        self.assertEqual(r['counts']['failed'], 5)

    def test_success_for_different_user_not_correlated(self):
        r = analyze([line(i) for i in range(5)] + [line(6, 'Accepted', user='other')])
        self.assertEqual(len(r['alerts']), 1)

    def test_hosts_are_isolated(self):
        self.assertFalse(analyze([line(i, host=str(i)) for i in range(5)])['alerts'])

    def test_sources_are_isolated(self):
        self.assertFalse(analyze([line(i, ip=f'192.0.2.{i+1}') for i in range(5)])['alerts'])

    def test_window_expires(self):
        self.assertFalse(analyze([line(0), line(10)], threshold=2, window_seconds=9)['alerts'])

    def test_inclusive_boundary(self):
        self.assertEqual(len(analyze([line(0), line(10)], 2, 10)['alerts']), 1)

    def test_duplicate_alert_suppression(self):
        self.assertEqual(len(analyze([line(i) for i in range(10)])['alerts']), 1)

    def test_new_burst_after_gap(self):
        r = analyze([line(i) for i in range(5)] + [line(i, minute=10) for i in range(5)])
        self.assertEqual(len(r['alerts']), 2)

    def test_unsorted_input(self):
        r = analyze(list(reversed([line(i) for i in range(5)] + [line(6, 'Accepted')])))
        self.assertEqual(len(r['alerts']), 2)

    def test_ipv6_normalized(self):
        r = analyze([line(0, ip='2001:db8::1'), line(1, ip='2001:0db8:0:0:0:0:0:1')], 2)
        self.assertEqual(r['alerts'][0]['source_ip'], '2001:db8::1')

    def test_invalid_ip_and_naive_timestamp(self):
        r = analyze([line(ip='999.1.1.1'), line().replace('+00:00', ''), line(2)])
        self.assertEqual(r['counts']['malformed'], 2)

    def test_unrelated_daemon_and_invalid_user_notice_not_counted(self):
        r = analyze([line().replace('sshd[42]', 'app[42]'),
                     '2026-11-10T09:00:00Z lab sshd[42]: Invalid user guest from 192.0.2.1 port 2222'])
        self.assertEqual(r['counts']['parsed'], 0)

    def test_failed_invalid_user_counted_once(self):
        r = analyze([line().replace('for student', 'for invalid user guest')])
        self.assertEqual(r['counts']['failed'], 1)

    def test_timezone_equivalence(self):
        a = line().replace('09:00:00+00:00', '14:00:00+05:00')
        self.assertEqual(len(analyze([a, line(1)], 2, 2)['alerts']), 1)

    def test_limits_and_parameters(self):
        for kwargs in [{'threshold':1}, {'window_seconds':0}, {'max_lines':1}]:
            with self.assertRaises(ValueError): analyze([line(), line(1)], **kwargs)

    def test_cli_output_and_exit(self):
        with tempfile.TemporaryDirectory() as folder:
            p = Path(folder)/'demo.log'
            p.write_text('\n'.join(line(i) for i in range(5)), encoding='utf-8')
            cmd = [sys.executable, str(Path(__file__).with_name('analyzer.py')), str(p), '--fail-on-alert']
            result = subprocess.run(cmd, text=True, capture_output=True)
            self.assertEqual(result.returncode, 1)
            self.assertEqual(json.loads(result.stdout)['counts']['parsed'], 5)

    def test_unsupported_input_exit_two(self):
        with tempfile.TemporaryDirectory() as folder:
            p=Path(folder)/'other.log'; p.write_text('unrelated\n')
            r=subprocess.run([sys.executable,str(Path(__file__).with_name('analyzer.py')),str(p)],capture_output=True)
            self.assertEqual(r.returncode,2)


if __name__ == '__main__': unittest.main()
