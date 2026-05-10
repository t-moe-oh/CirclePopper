#!/bin/bash
set -e

echo "=== Triggering Circle Popper iOS build via GitHub Actions ==="
echo ""

if ! command -v gh &>/dev/null; then
    echo "❌ GitHub CLI 'gh' not found."
    echo "   Install: sudo pacman -S github-cli"
    exit 1
fi

echo "Pushing latest code..."
git push

echo ""
echo "Triggering workflow..."
gh workflow run ios-build.yml

echo ""
echo "Waiting for build to start..."
sleep 5

RUN_ID=$(gh run list --workflow ios-build.yml --limit 1 --json databaseId -q ".[].databaseId")
echo "Build started: https://github.com/t-moe-oh/CirclePopper/actions/runs/$RUN_ID"
echo ""

echo "Watching build..."
gh run watch "$RUN_ID"

echo ""
echo "Downloading artifact..."
gh run download "$RUN_ID" --name CirclePopper.ipa

echo ""
echo "✅ IPA downloaded: CirclePopper.ipa"
echo "   Sideload with: iloader install --ipa CirclePopper.ipa"
