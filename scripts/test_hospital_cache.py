"""Comprueba la última búsqueda sin conexión en el emulador y restaura sus radios."""
import json, os, subprocess, time, xml.etree.ElementTree as ET
from pathlib import Path

ADB = os.environ.get('ADB', str(Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe'))
OUT = Path(__file__).resolve().parents[1]/'evidence'/'hospital-fix'

def adb(*args):
    result=subprocess.run([ADB,*args],capture_output=True,timeout=30)
    if result.returncode: raise RuntimeError(result.stderr.decode(errors='replace'))
    return result.stdout.decode(errors='replace').strip()

wifi=adb('shell','settings','get','global','wifi_on')
data=adb('shell','settings','get','global','mobile_data')
try:
    adb('shell','am','force-stop','co.edu.ucc.reto9')
    adb('shell','svc','wifi','disable')
    adb('shell','svc','data','disable')
    adb('shell','am','start','-n','co.edu.ucc.reto9/.MainActivity')
    current=''
    for _ in range(25):
        time.sleep(2)
        adb('shell','uiautomator','dump','/sdcard/hospital-cache.xml')
        xml=adb('shell','cat','/sdcard/hospital-cache.xml')
        current='\n'.join(n.get('text','') for n in ET.fromstring(xml).iter('node'))
        if 'No se pudo actualizar' in current: break
    assert 'Datos guardados del' in current,current
    assert '20.0 km' in current,current
    first_hospital=json.loads((OUT/'hospital-live.json').read_text(encoding='utf-8'))['hospitals'][0]['name']
    assert first_hospital in current,current
    adb('shell','screencap','-p','/sdcard/hospital-cache.png')
    adb('pull','/sdcard/hospital-cache.png',str(OUT/'hospital-cache.png'))
    (OUT/'hospital-cache.xml').write_text(xml,encoding='utf-8')
    (OUT/'cache-test.txt').write_text('PASS | Sin red se conservan los hospitales de la misma búsqueda\nPASS | Se identifica la fecha de los datos guardados\nPASS | Origen y radio de 20 km conservados\n',encoding='utf-8')
    print('PASS: hospitales guardados visibles y estado sin conexión explícito')
finally:
    if wifi=='1': adb('shell','svc','wifi','enable')
    if data=='1': adb('shell','svc','data','enable')
    adb('shell','am','force-stop','co.edu.ucc.reto9')
    time.sleep(2)
    adb('shell','am','start','-n','co.edu.ucc.reto9/.MainActivity')
