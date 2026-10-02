#!/usr/bin/env python3
"""
Corrige la regle Sonar d'accessibilite (WCAG) : "Add an id attribute to this input field
and associate it with a label."
Pour chaque  <label>Texte</label> suivi d'un <input|select|textarea> , ajoute
for="..." sur le label et id="..." sur le champ. Idempotent.
Usage (racine du depot) : python3 ci/fix-frontend-a11y.py
"""
import pathlib
import re

ROOT = pathlib.Path("frontend/src/app/pages")
PAT = re.compile(r"<label>([^<]*)</label>(\s*)<(input|select|textarea)\b")
total = 0
for f in sorted(ROOT.glob("*/*.html")):
    src = f.read_text(encoding="utf-8")
    page = f.parent.name
    n = [0]

    def repl(m):
        n[0] += 1
        ident = f"{page}-champ-{n[0]}"
        return f'<label for="{ident}">{m.group(1)}</label>{m.group(2)}<{m.group(3)} id="{ident}"'

    new = PAT.sub(repl, src)
    if n[0]:
        f.write_text(new, encoding="utf-8")
        total += n[0]
        print(f"  {f.relative_to(ROOT)} : {n[0]} champ(s) associe(s) a leur label")
print(f"==> {total} champs corriges")
