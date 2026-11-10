# Cybersecurity learning labs

Two small Python tools for practicing defensive security and explaining technical decisions in an internship interview. Both run offline and use only the Python standard library.

| Project | What it demonstrates | Tests |
| --- | --- | --- |
| [SSH Log Analyzer](ssh_log_analyzer/) | Parsing, sliding time windows, login-event correlation, IPv4 and IPv6 | 17 |
| [SSH Configuration Audit](ssh_config_audit/) | Explicit security baselines, effective configuration, handling incomplete evidence | 12 |

## Run everything

Use Python 3.10 or newer. Tested locally with Python 3.12.14.
From this directory:

```sh
python3 -m unittest discover -s ssh_log_analyzer -v
python3 -m unittest discover -s ssh_config_audit -v
python3 ssh_log_analyzer/analyzer.py ssh_log_analyzer/samples/auth.log
python3 ssh_config_audit/audit.py ssh_config_audit/samples/review.txt
```

Sample data is synthetic and uses documentation IP addresses. The programs do not connect to servers, modify configuration or block IPs. Real authentication logs and configuration exports should stay outside this public repository.

## Development notes

The initial implementation was created with AI assistance as a learning portfolio. The code, tests and limitations are available for review. This repository does not claim commercial deployments, workplace incident response or independent authorship of every line.

See [the Russian study guide](STUDY_GUIDE_RU.md) for exercises and interview questions.

## Project location

These self-contained labs are published in the `portfolio/cybersecurity-labs` branch of the existing course repository. Each project directory can later be moved into its own repository without code changes. The purchase-tracker course project is a separate subject.
