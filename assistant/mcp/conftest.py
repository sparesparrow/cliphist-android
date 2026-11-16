import pytest


@pytest.fixture(autouse=True)
def _private_phase_file(tmp_path, monkeypatch):
    """Turn stages (state/phase) written during tests go to tmp, never the live state dir."""
    monkeypatch.setenv("VA_PHASE_FILE", str(tmp_path / "phase"))
