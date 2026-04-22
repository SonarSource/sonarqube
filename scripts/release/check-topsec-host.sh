#!/usr/bin/env bash

set -euo pipefail

REQUIRED_VM_MAX_MAP_COUNT=262144
RECOMMENDED_VM_MAX_MAP_COUNT=524288

current_vm_max_map_count="$(sysctl -n vm.max_map_count 2>/dev/null || true)"

if [[ -z "${current_vm_max_map_count}" ]]; then
  echo "Unable to read vm.max_map_count from the host." >&2
  echo "Please run the following commands manually before starting Docker Compose:" >&2
  echo "  sudo sysctl -w vm.max_map_count=${RECOMMENDED_VM_MAX_MAP_COUNT}" >&2
  echo "  echo 'vm.max_map_count=${RECOMMENDED_VM_MAX_MAP_COUNT}' | sudo tee /etc/sysctl.d/99-topsec-ai-audit.conf" >&2
  echo "  sudo sysctl --system" >&2
  exit 1
fi

if (( current_vm_max_map_count < REQUIRED_VM_MAX_MAP_COUNT )); then
  echo "Host kernel parameter vm.max_map_count=${current_vm_max_map_count} is too low for Elasticsearch." >&2
  echo "Required minimum: ${REQUIRED_VM_MAX_MAP_COUNT}" >&2
  echo "Recommended value: ${RECOMMENDED_VM_MAX_MAP_COUNT}" >&2
  echo >&2
  echo "Fix the host and rerun deployment:" >&2
  echo "  sudo sysctl -w vm.max_map_count=${RECOMMENDED_VM_MAX_MAP_COUNT}" >&2
  echo "  echo 'vm.max_map_count=${RECOMMENDED_VM_MAX_MAP_COUNT}' | sudo tee /etc/sysctl.d/99-topsec-ai-audit.conf" >&2
  echo "  sudo sysctl --system" >&2
  exit 1
fi

echo "Host check passed: vm.max_map_count=${current_vm_max_map_count}"
