# SSH Log Analyzer

An offline command-line tool that turns supported OpenSSH authentication messages into reviewable JSON. It identifies a burst of failed logins from the same source to the same host, then separately checks for a successful login after repeated failures for the same account.

## Quick start

From this directory, using Python 3.10 or newer:

```sh
python3 analyzer.py samples/auth.log
python3 analyzer.py samples/auth.log --threshold 5 --window 300 --fail-on-alert
python3 -m unittest discover -v
```

The supplied example produces 7 parsed authentication events, 5 failures, 2 accepted logins and 2 triage alerts. One unrelated message is ignored. The full output is in [samples/expected.json](samples/expected.json).

## Input contract

Supported records have this shape:

```text
2026-11-10T09:00:00Z lab-server sshd[1200]: Failed password for student from 192.0.2.10 port 41100 ssh2
```

The timestamp must use ISO 8601 and include an offset or `Z`. Common `journalctl -o short-iso` output follows this general shape, but check the actual export against the example. `sshd`, `sshd-session` and `sshd-auth` process labels are recognised. Password, public-key and keyboard-interactive authentication messages are supported.

Traditional month-name syslog timestamps, JSON journals and unrelated log formats are not parsed. Unsupported lines are counted as ignored. Malformed recognised authentication records are counted separately; no successful parse is an error, not a clean security result.

## Detection decisions

- Events are sorted by timestamp before analysis; ties retain input order.
- A sliding window groups failures by destination hostname and source IP.
- A single burst alert is emitted when the threshold is first reached. Another is possible after the count drops below the threshold.
- Success-after-failures also requires the same username. A successful login to another account is not automatically linked.
- A failure exactly at the window boundary is included.
- Equivalent IPv6 representations are normalised.
- Standalone `Invalid user` messages are ignored so a paired `Failed password for invalid user` line is not counted twice.

## Limits

A triage alert is not proof of compromise. Shared source addresses, user mistakes, duplicate collected logs and incomplete coverage can change the interpretation. Duplicate input records are not deduplicated. The detector does not identify distributed attacks that stay below the per-source threshold, parse every OpenSSH message or inspect activity after authentication. Review ignored and malformed counts.

Input is limited to 200,000 lines. Parsed events are held in memory for sorting. This is a teaching tool for small exported logs, not a streaming SIEM.

Exit codes: `0` for a completed report, `1` for alerts when `--fail-on-alert` is enabled, `2` for input or argument errors. JSON output escapes untrusted text rather than printing raw usernames into the terminal.

## Test coverage

17 tests cover correlation boundaries, host and source separation, sliding-window expiry, alert suppression, unsorted input, IPv6, timezones, malformed input and CLI behaviour. Run the command above; the test count describes the current source, not an externally hosted CI result.

## References

- [OpenSSH daemon manual](https://man.openbsd.org/sshd)
- [Python ipaddress](https://docs.python.org/3/library/ipaddress.html)
- [Python datetime](https://docs.python.org/3/library/datetime.html)
