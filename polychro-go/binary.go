// Copyright 2026 Naftiko — Apache License 2.0

package polychro

import (
	"errors"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
)

// BinaryNotFoundError indicates that the Polychro native binary cannot be located.
var BinaryNotFoundError = errors.New("polychro binary not found")

func platformBinaryName() string {
	if runtime.GOOS == "windows" {
		return "polychro.exe"
	}
	return "polychro"
}

// ResolveBinary locates the Polychro native binary.
//
// Search order:
//  1. POLYCHRO_BIN environment variable
//  2. Adjacent bin/ directory (bundled distribution)
//  3. Binary on PATH
func ResolveBinary() (string, error) {
	// 1. Environment variable
	if envBin := os.Getenv("POLYCHRO_BIN"); envBin != "" {
		if _, err := os.Stat(envBin); err == nil {
			return envBin, nil
		}
	}

	// 2. Adjacent bin/ directory
	execPath, err := os.Executable()
	if err == nil {
		candidate := filepath.Join(filepath.Dir(execPath), "bin", platformBinaryName())
		if _, err := os.Stat(candidate); err == nil {
			return candidate, nil
		}
	}

	// 3. On PATH
	if path, err := exec.LookPath("polychro"); err == nil {
		return path, nil
	}

	return "", BinaryNotFoundError
}
