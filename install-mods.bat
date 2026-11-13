@echo off
rem Minecraft Lab mod installer for Windows. Keep only this one file: it downloads the real installer.
title Minecraft Lab mod installer
powershell -NoProfile -ExecutionPolicy Bypass -Command "irm https://raw.githubusercontent.com/huydangtruong/minecraft-lab/main/install-mods.ps1 | iex"
pause
