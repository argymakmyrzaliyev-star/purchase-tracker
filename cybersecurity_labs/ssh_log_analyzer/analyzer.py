"""Offline OpenSSH log triage. Uses only the Python standard library."""
from __future__ import annotations

import argparse
from collections import Counter, defaultdict, deque
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
import ipaddress
import json
from pathlib import Path
import re
from typing import Iterable

PREFIX = re.compile(r'^(\S+)\s+(\S+)\s+sshd(?:-session|-auth)?\[\d+\]:\s+(.*)$')
AUTH = re.compile(r'^(Failed|Accepted) (password|publickey|keyboard-interactive(?:/pam)?) for (invalid user )?(\S+) from (\S+) port (\d+)(?:\s|$)')


@dataclass(frozen=True)
class Event:
    time: datetime
    host: str
    address: str
    user: str
    success: bool
    line: int


def parse_event(line: str, number: int) -> Event | None:
    """Return an event, ignore other messages, and reject malformed auth events."""
    prefix = PREFIX.match(line)
    if not prefix:
        return None
    timestamp, host, message = prefix.groups()
    match = AUTH.match(message)
    if not match:
        return None
    outcome, method, invalid, user, address, port = match.groups()
    parsed_time = datetime.fromisoformat(timestamp.replace('Z', '+00:00'))
    if parsed_time.tzinfo is None or parsed_time.utcoffset() is None:
        raise ValueError('Authentication timestamps must include a timezone')
    if not 1 <= int(port) <= 65535:
        raise ValueError('Invalid source port')
    if outcome == 'Accepted' and invalid:
        raise ValueError('Accepted event cannot identify an invalid user')
    return Event(parsed_time.astimezone(timezone.utc), host,
                 str(ipaddress.ip_address(address)), user, outcome == 'Accepted', number)


def analyze(lines: Iterable[str], threshold: int = 5, window_seconds: int = 300,
            max_lines: int = 200_000) -> dict:
    if threshold < 2 or window_seconds < 1 or max_lines < 1:
        raise ValueError('threshold >= 2, window_seconds >= 1 and max_lines >= 1 required')
    events = []
    malformed, ignored, total = 0, 0, 0
    for total, line in enumerate(lines, 1):
        if total > max_lines:
            raise ValueError(f'Input exceeds {max_lines} lines; split the log first')
        try:
            event = parse_event(line.rstrip('\n'), total)
        except (ValueError, OverflowError):
            malformed += 1
            continue
        if event is None:
            ignored += 1
        else:
            events.append(event)
    # Input may contain merged or out-of-order log records.
    events.sort(key=lambda e: (e.time, e.line))
    failures = defaultdict(deque)
    failures_by_user = defaultdict(deque)
    active_burst = defaultdict(bool)
    alerts = []
    totals = Counter()
    delta = timedelta(seconds=window_seconds)
    for e in events:
        source = (e.host, e.address)
        account = (e.host, e.address, e.user)
        history = failures[source]
        user_history = failures_by_user[account]
        cutoff = e.time - delta
        # Boundary is inclusive: an event exactly N seconds old still counts.
        while history and history[0] < cutoff:
            history.popleft()
        while user_history and user_history[0] < cutoff:
            user_history.popleft()
        if len(history) < threshold:
            active_burst[source] = False
        if e.success:
            totals['accepted'] += 1
            if len(user_history) >= threshold:
                alerts.append({'type': 'success_after_failures', 'host': e.host,
                               'source_ip': e.address, 'user': e.user,
                               'time': e.time.isoformat(), 'failures_in_window': len(user_history),
                               'line': e.line})
        else:
            totals['failed'] += 1
            history.append(e.time)
            user_history.append(e.time)
            if len(history) >= threshold and not active_burst[source]:
                active_burst[source] = True
                alerts.append({'type': 'failure_burst', 'host': e.host,
                               'source_ip': e.address, 'window_start': history[0].isoformat(),
                               'threshold_reached_at': e.time.isoformat(),
                               'failures_at_threshold': len(history), 'line': e.line})
    return {'schema_version': 1, 'purpose': 'triage candidates, not proof of compromise',
            'parameters': {'failure_threshold': threshold, 'window_seconds': window_seconds},
            'counts': {'lines': total, 'parsed': len(events), 'ignored': ignored,
                       'malformed': malformed, 'failed': totals['failed'],
                       'accepted': totals['accepted']}, 'alerts': alerts}


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('log', type=Path, help='OpenSSH log with ISO 8601 timezone-bearing timestamps')
    parser.add_argument('--threshold', type=int, default=5)
    parser.add_argument('--window', type=int, default=300, help='Sliding window in seconds')
    parser.add_argument('--fail-on-alert', action='store_true', help='Exit 1 when triage candidates exist')
    args = parser.parse_args(argv)
    try:
        with args.log.open(encoding='utf-8') as stream:
            report = analyze(stream, args.threshold, args.window)
        if report['counts']['parsed'] == 0:
            raise ValueError('No supported authentication records; check input format')
    except (OSError, UnicodeError, ValueError) as error:
        parser.error(str(error))
    print(json.dumps(report, indent=2, ensure_ascii=True))
    return 1 if args.fail_on_alert and report['alerts'] else 0


if __name__ == '__main__':
    raise SystemExit(main())
