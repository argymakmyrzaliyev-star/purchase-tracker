"""Compare an offline sshd -T snapshot with a documented learning baseline."""
from __future__ import annotations
import argparse
from collections import Counter
from dataclasses import dataclass
import json
from pathlib import Path


@dataclass(frozen=True)
class Rule:
    key: str
    expected: str
    reason: str
    upper_bound: int | None = None


RULES = (
    Rule('permitrootlogin', 'no', 'Use named administrative accounts.'),
    Rule('passwordauthentication', 'no', 'This profile uses public-key authentication.'),
    Rule('permitemptypasswords', 'no', 'Do not allow empty passwords.'),
    Rule('pubkeyauthentication', 'yes', 'Public keys are required by this profile.'),
    Rule('hostbasedauthentication', 'no', 'This profile does not use host-based trust.'),
    Rule('ignorerhosts', 'yes', 'Ignore legacy per-user host trust files.'),
    Rule('strictmodes', 'yes', 'Check relevant file ownership and permissions.'),
    Rule('x11forwarding', 'no', 'This profile does not require remote graphical applications.'),
    Rule('maxauthtries', '1..4', 'Limit authentication attempts per connection.', 4),
    Rule('logingracetime', '1..60 seconds', 'Limit time spent waiting for authentication.', 60),
)
KEYS = {r.key for r in RULES}


def parse_snapshot(text: str) -> dict[str, str]:
    values = {}
    if len(text) > 2_000_000:
        raise ValueError('Snapshot exceeds the 2 MB character limit')
    for number, raw in enumerate(text.splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith('#'):
            continue
        fields = line.split(None, 1)
        if len(fields) != 2:
            raise ValueError(f'Line {number}: expected key and value')
        key, value = fields[0].lower(), fields[1].strip()
        if key in {'match', 'include'}:
            raise ValueError('Use evaluated sshd -T output, not raw Match/Include configuration')
        if key in KEYS:
            if key in values:
                raise ValueError(f'Line {number}: duplicate checked directive {key}')
            values[key] = value
    if not values:
        raise ValueError('No checked SSH settings found')
    return values


def audit(text: str) -> dict:
    values = parse_snapshot(text)
    findings = []
    for rule in RULES:
        actual = values.get(rule.key)
        if actual is None:
            status = 'unknown'
        elif rule.upper_bound is not None:
            status = ('pass' if actual.isascii() and actual.isdecimal()
                      and 1 <= int(actual) <= rule.upper_bound else 'review')
        else:
            status = 'pass' if actual == rule.expected else 'review'
        findings.append({'setting': rule.key, 'observed': actual,
                         'expected': rule.expected, 'status': status, 'reason': rule.reason})
    counts = Counter(f['status'] for f in findings)
    return {'schema_version': 1, 'profile': 'learning-key-based-admin-v1',
            'scope': 'Provided effective snapshot and connection context only',
            'verdict': 'matches_baseline' if counts['pass'] == len(RULES) else 'review_required',
            'counts': {k: counts[k] for k in ('pass','review','unknown')},
            'findings': findings,
            'limitations': ['Not a vulnerability scan or compliance certification.',
                            'A snapshot cannot prove what the running daemon currently uses.',
                            'AuthenticationMethods, PAM, MFA and other contexts require separate review.']}


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('snapshot', type=Path)
    parser.add_argument('--fail-on-review', action='store_true')
    args = parser.parse_args(argv)
    try:
        with args.snapshot.open(encoding='utf-8') as stream:
            report = audit(stream.read(2_000_001))
    except (OSError, UnicodeError, ValueError) as error:
        parser.error(str(error))
    print(json.dumps(report, indent=2, ensure_ascii=True))
    return 1 if args.fail_on_review and report['verdict'] != 'matches_baseline' else 0


if __name__ == '__main__': raise SystemExit(main())
