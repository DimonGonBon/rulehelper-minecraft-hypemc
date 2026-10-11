# RuleHelper для Minecraft

Клиентский Fabric-мод в двух сборках: Minecraft 1.21.11 и 1.21.4.

| Minecraft | Исходники | Готовый JAR |
| --- | --- | --- |
| 1.21.11 | `versions/1.21.11` | `artifacts/RuleHelper-1.21.11-Universal-DonationSelect.jar` |
| 1.21.4 | `versions/1.21.4` | `artifacts/RuleHelper-1.21.4-Universal-DonationSelect.jar` |

Включены выбор ника в чате, правила и команды наказаний, разбан/размут, выбор группы `Правитель+` или `Staff-`, а также функция `/pex user <цель>`. Для неё мод последовательно отправляет `op <сохранённый ник>`, затем `pex user <цель>`; право на команды проверяет сервер.

Обе версии собираются с JDK 21 через собственный Gradle wrapper. GitHub Actions автоматически соберёт обе версии после push или pull request и приложит JAR как workflow artifacts. Готовые JAR также находятся в `artifacts/`.

Исходники восстановлены из доступных JAR-файлов и адаптированы для указанных версий Minecraft, поэтому могут отличаться от исходного проекта автора.
