# Testing Instructions for Clipboard History Android App

## Overview
This document provides instructions for testing the implemented improvements in the Clipboard History Android app. The app includes drag-and-drop smart actions, memory leak fixes, race condition fixes, enhanced content analysis, and other enhancements.

## Prerequisites
- Android device with Android API 24+ (Android 7.0+)
- USB debugging enabled
- ADB installed
- Download the APK from: [GitHub Releases](https://github.com/sparesparrow/cliphist-android/releases)

## Installation Steps
1. Download the APK file from the GitHub release
2. Enable "Install unknown apps" for your browser or file manager
3. Install the APK on your device
4. Grant all requested permissions (Accessibility, Notification, etc.)

## Testing Checklist

### 1. Basic Functionality
- [ ] App launches without crashing
- [ ] Main screen displays clipboard history
- [ ] Can copy text and see it appear in history
- [ ] Settings are accessible

### 2. Accessibility Service (Floating Bubbles)
- [ ] Enable Accessibility Service in Android Settings > Accessibility
- [ ] Copy text - floating bubble appears
- [ ] Multiple bubbles can be shown simultaneously
- [ ] Bubbles don't cause memory issues (no crashes after multiple copies)

### 3. Drag-and-Drop Smart Actions
- [ ] When bubble is visible, drag it to different areas
- [ ] Action areas are visually indicated (highlighted rectangles)
- [ ] Dragging to different edges shows different smart actions
- [ ] Smart actions work (e.g., phone numbers open dialer, URLs open browser)

### 4. Content Analysis Improvements
- [ ] Phone numbers are correctly detected (including international formats)
- [ ] URLs are correctly identified
- [ ] Email addresses are detected
- [ ] Maps links are recognized
- [ ] No false positives for content type detection

### 5. Performance and Stability
- [ ] App doesn't crash after extended use
- [ ] Memory usage is stable (no gradual increase)
- [ ] No race conditions when copying rapidly
- [ ] Smooth animations and transitions

### 6. UI/UX Improvements
- [ ] All UI components load correctly
- [ ] No missing icons or broken layouts
- [ ] Dark/light theme support (if applicable)
- [ ] Proper error handling and user feedback

## Feedback Questions

Please answer the following questions after testing:

1. **Overall Experience**: How would you rate the app's stability? (1-5 scale)
   - 1: Very unstable, many crashes
   - 5: Completely stable, no issues

2. **Smart Actions**: How accurate and useful are the smart actions?
   - Are they triggered correctly?
   - Do they perform the expected action?

3. **Content Analysis**: Did the app correctly identify content types?
   - Any false positives or negatives?
   - Examples of correctly/incorrectly identified content?

4. **Performance**: Any performance issues?
   - Slow response times?
   - Memory leaks?
   - Battery drain?

5. **UI/UX**: How is the user interface?
   - Intuitive navigation?
   - Visual appeal?
   - Accessibility?

6. **Bugs Found**: List any bugs or unexpected behavior you encountered.

7. **Suggestions**: What improvements would you suggest?

## Reporting Issues
If you find any issues, please create an issue on GitHub with:
- Device model and Android version
- Steps to reproduce
- Expected vs actual behavior
- Screenshots if applicable

---

# Testovací instrukce pro aplikaci Clipboard History Android

## Přehled
Tento dokument poskytuje instrukce pro testování implementovaných vylepšení v aplikaci Clipboard History pro Android. Aplikace obsahuje drag-and-drop chytré akce, opravy úniků paměti, opravy race conditions, vylepšenou analýzu obsahu a další vylepšení.

## Předpoklady
- Android zařízení s Android API 24+ (Android 7.0+)
- Povolené USB ladění
- Nainstalované ADB
- Stáhněte APK z: [GitHub Releases](https://github.com/sparesparrow/cliphist-android/releases)

## Kroky instalace
1. Stáhněte soubor APK z GitHub release
2. Povolte "Instalovat neznámé aplikace" pro váš prohlížeč nebo správce souborů
3. Nainstalujte APK na vaše zařízení
4. Udělte všechna požadovaná oprávnění (Přístupnost, Oznámení, atd.)

## Testovací checklist

### 1. Základní funkčnost
- [ ] Aplikace se spustí bez pádu
- [ ] Hlavní obrazovka zobrazuje historii schránky
- [ ] Lze kopírovat text a vidět ho v historii
- [ ] Nastavení jsou přístupná

### 2. Služba přístupnosti (plovoucí bubliny)
- [ ] Povolte službu přístupnosti v Nastavení Android > Přístupnost
- [ ] Kopírování textu - objeví se plovoucí bublina
- [ ] Lze zobrazit více bublin současně
- [ ] Bubliny nezpůsobují problémy s pamětí (žádné pády po více kopírováních)

### 3. Drag-and-drop chytré akce
- [ ] Když je bublina viditelná, táhněte ji do různých oblastí
- [ ] Akční oblasti jsou vizuálně indikovány (zvýrazněné obdélníky)
- [ ] Táhnutí na různé hrany zobrazuje různé chytré akce
- [ ] Chytré akce fungují (např. telefonní čísla otevřou vytáčení, URL otevřou prohlížeč)

### 4. Vylepšení analýzy obsahu
- [ ] Telefonní čísla jsou správně detekována (včetně mezinárodních formátů)
- [ ] URL jsou správně identifikovány
- [ ] Emailové adresy jsou detekovány
- [ ] Odkazy na mapy jsou rozpoznány
- [ ] Žádné falešné pozitivy pro detekci typu obsahu

### 5. Výkon a stabilita
- [ ] Aplikace se nezhroutí po delším používání
- [ ] Využití paměti je stabilní (žádný postupný nárůst)
- [ ] Žádné race conditions při rychlém kopírování
- [ ] Plynulé animace a přechody

### 6. Vylepšení UI/UX
- [ ] Všechny UI komponenty se načítají správně
- [ ] Žádné chybějící ikony nebo rozbitá rozložení
- [ ] Podpora tmavého/světlého tématu (pokud aplikovatelné)
- [ ] Správné zpracování chyb a zpětná vazba uživateli

## Otázky pro zpětnou vazbu

Odpovězte prosím na následující otázky po testování:

1. **Celkový zážitek**: Jak byste ohodnotili stabilitu aplikace? (škála 1-5)
   - 1: Velmi nestabilní, mnoho pádů
   - 5: Úplně stabilní, žádné problémy

2. **Chytré akce**: Jak přesné a užitečné jsou chytré akce?
   - Spouštějí se správně?
   - Provádějí očekávanou akci?

3. **Analýza obsahu**: Správně aplikace identifikovala typy obsahu?
   - Nějaké falešné pozitivy nebo negativy?
   - Příklady správně/nesprávně identifikovaného obsahu?

4. **Výkon**: Nějaké problémy s výkonem?
   - Pomalé časy odezvy?
   - Úniky paměti?
   - Vybití baterie?

5. **UI/UX**: Jak je uživatelské rozhraní?
   - Intuitivní navigace?
   - Vizuální přitažlivost?
   - Přístupnost?

6. **Nalezené chyby**: Uveďte všechny chyby nebo neočekávané chování, které jste narazili.

7. **Návrhy**: Jaká vylepšení byste navrhli?

## Hlášení problémů
Pokud najdete nějaké problémy, vytvořte prosím issue na GitHub s:
- Modelem zařízení a verzí Android
- Kroky k reprodukci
- Očekávané vs skutečné chování
- Snímky obrazovky, pokud je to možné