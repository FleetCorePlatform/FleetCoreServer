[![Build and Publish](https://img.shields.io/github/actions/workflow/status/FleetCorePlatform/FleetCoreServer/publish-docker.yml?branch=main)](https://github.com/FleetCorePlatform/FleetCoreServer/actions)
[![Latest Release](https://img.shields.io/github/v/release/FleetCorePlatform/FleetCoreServer)](https://github.com/FleetCorePlatform/FleetCoreServer/releases/latest)

# FleetCoreServer
This is the backend component of the [FleetCore Platform](https://github.com/FleetCorePlatform)

## System Overview
FleetCoreServer operates as the central coordination hub for the whole ecosystem.
- **Hardware Provisioning**: Registers drones and establishes AWS IoT and Kinesis signaling channels.
- **Mission Orchestration**: Compiles and dispatches missions via AWS IoT Jobs.
- **State Management**: Persists telemetry, detection events, and geographic Outpost configurations.


> **ℹ️ Note:** This README serves as a quick entry point. For the complete documentation, including user manuals and technical specifications, please visit our **[Official Documentation Site](https://fleetcoreplatform.github.io/Deployment)**.
## Docker Setup

The deployment configuration is defined in [compose.yml](src/main/docker/compose.yml).

1. Review the file and define all required environment variables.
2. Provide valid AWS credentials, database connection strings, and OIDC parameters prior to initialization.
3. Start the backend container:

```bash
docker compose -f src/main/docker/compose.yml up -d
```