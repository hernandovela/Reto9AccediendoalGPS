"""Sondeo HTTPS real, sin desactivar certificados ni inventar resultados."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import json, subprocess, time

OUT = Path(__file__).resolve().parents[1] / 'evidence' / 'hospital-fix'
OUT.mkdir(parents=True, exist_ok=True)
QUERY = '[out:json][timeout:40];(nwr[amenity=hospital](4.5527,-74.2378,4.9125,-73.8766);nwr[healthcare=hospital](4.5527,-74.2378,4.9125,-73.8766););out center tags;'
HOSTS = {
    'main': 'https://overpass-api.de/api/interpreter',
    'coffee': 'https://overpass.private.coffee/api/interpreter',
    'vk': 'https://maps.mail.ru/osm/tools/overpass/api/interpreter',
}

def probe(item):
    name, url = item
    start = time.monotonic()
    file = OUT / (name + '.json')
    result = subprocess.run(['curl.exe', '--silent', '--show-error', '--connect-timeout', '10',
        '--max-time', '55', '--user-agent', 'Reto9AccediendoalGPS/1.1', '--data-urlencode',
        'data=' + QUERY, '--output', str(file), '--write-out', '%{http_code}', url],
        capture_output=True, text=True, timeout=60)
    record = dict(server=name, http=result.stdout, seconds=round(time.monotonic()-start,2), error=result.stderr)
    try:
        body=json.loads(file.read_text(encoding='utf-8'))
        record['count']=len(body.get('elements',[]))
        record['remark']=body.get('remark')
    except Exception:
        pass
    return record

if __name__ == '__main__':
    with ThreadPoolExecutor(max_workers=3) as pool:
        records=list(pool.map(probe,HOSTS.items()))
    (OUT/'servers.json').write_text(json.dumps(records,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(records,ensure_ascii=False,indent=2))
