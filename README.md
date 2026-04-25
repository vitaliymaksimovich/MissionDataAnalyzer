# MissionDataAnalyzer

[![Java](https://img.shields.io/badge/Java-17+-orange)]()
[![Maven](https://img.shields.io/badge/Maven-build-blue)]()
[![Release](https://img.shields.io/github/v/release/vitaliymaksimovich/MissionDataAnalyzer)](https://github.com/vitaliymaksimovich/MissionDataAnalyzer/releases)
[![AI](https://img.shields.io/badge/AI-GigaChat-blue)]()

Приложение для анализа миссий с поддержкой AI-обзора через GigaChat.  
Позволяет загружать миссии в формате **TXT**, **JSON** или **XML**, отображать структурированные данные, получать краткий AI-отчёт и экспортировать результаты в **HTML** и **TXT**.



---

## Особенности

- Поддержка форматов: `TXT`, `JSON`, `XML`
- Единая внутренняя модель данных `Mission`
- Swing GUI для выбора файла и отображения отчётов
- AI-анализ миссии с помощью GigaChat
- Экспорт отчёта в HTML (A4-стиль) и TXT
- Работа даже без AI-ключа (fallback mode)
- Поддержка комментариев и необработанных данных

---

## Требования

- Java 17 или выше
- Maven (для сборки из исходников)
- Переменная окружения `GIGACHAT_AUTH_KEY` для AI-обзора (необязательно)

---

## Запуск приложения

### 1. Если используешь готовый JAR

Скачайте `MissionDataAnalyzer-1.0.jar`, и файл-пример из [Releases](#) и выполните:

```bash
java -jar MissionDataAnalyzer-1.0.jar
