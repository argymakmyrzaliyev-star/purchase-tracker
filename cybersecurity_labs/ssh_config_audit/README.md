# SSH Configuration Audit

An offline comparison of an effective OpenSSH server configuration snapshot against ten explicit rules. It reports `pass`, `review` or `unknown` for each setting and returns machine-readable JSON.

## Quick start

From this directory, with Python 3.10 or newer:

```sh
python3 audit.py samples/baseline.txt
python3 audit.py samples/review.txt --fail-on-review
python3 -m unittest discover -v
```

The baseline example matches all ten rules. The review example contains five deviations. Both are synthetic. Compare [expected-baseline.json](samples/expected-baseline.json) and [expected-review.json](samples/expected-review.json).

## Exporting a snapshot

On a machine you administer, OpenSSH can evaluate its configuration without starting a daemon:

```sh
sudo /usr/sbin/sshd -T > sshd-effective.txt
```

If `Match` blocks apply, export the specific connection context instead, substituting real values on your own machine:

```sh
sudo /usr/sbin/sshd -T -C user=student,host=lab-client,addr=192.0.2.20 > sshd-effective.txt
python3 audit.py sshd-effective.txt
```

The auditor itself does not invoke `sudo`, execute `sshd`, read system paths automatically or change settings. It only reads the supplied file. Keep real exports out of the public repository.

## Baseline

This is a learning profile for named administrators using public keys and no X11 forwarding. It is not a universal configuration or a CIS certification.

| Setting | Expected |
| --- | --- |
| PermitRootLogin | no |
| PasswordAuthentication | no |
| PermitEmptyPasswords | no |
| PubkeyAuthentication | yes |
| HostbasedAuthentication | no |
| IgnoreRhosts | yes |
| StrictModes | yes |
| X11Forwarding | no |
| MaxAuthTries | 1 through 4 |
| LoginGraceTime | 1 through 60 seconds |

A different value means review against the intended use, not automatic proof of a vulnerability. For example, `prohibit-password` still permits some root authentication and therefore differs from this profile's `no`. MFA deployments require their own review of `AuthenticationMethods`, PAM and keyboard-interactive settings.

## Avoiding false confidence

Raw `Match` or `Include` directives are rejected. The tool does not implement OpenSSH's configuration resolution. Missing checked values stay `unknown`; they are never replaced by assumed defaults. Duplicate checked settings are rejected as ambiguous. Repeated unchecked settings such as `ListenAddress` are allowed. Numeric values are expected as normalised seconds/counts from an evaluated snapshot.

A complete match applies only to the provided snapshot and context. It cannot prove that a running service has loaded those settings, that keys are protected or that the host is otherwise secure.

Exit codes: `0` for a completed report, `1` when `--fail-on-review` finds deviations or missing evidence, `2` for input errors. Input is limited to two million characters.

## Test coverage

12 tests cover a full match, missing data, alternative root policy, numeric boundaries, raw conditional configuration, duplicates, repeated unchecked fields, input validation and CLI output.

## References

- [OpenSSH sshd, especially -T and -C](https://man.openbsd.org/sshd)
- [OpenSSH server configuration reference](https://man.openbsd.org/sshd_config)
