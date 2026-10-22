"""Pruebas funcionales reales por ADB. Ejecutar con el emulador iniciado."""
import os, re, subprocess, time, sys, xml.etree.ElementTree as ET
from pathlib import Path

ADB = os.environ.get('ADB', str(Path.home() / 'AppData/Local/Android/Sdk/platform-tools/adb.exe'))
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'evidence'
OUT.mkdir(exist_ok=True)
report = []

def adb(*args):
    p = subprocess.run([ADB, *args], capture_output=True, timeout=55)
    if p.returncode:
        raise RuntimeError(p.stderr.decode(errors='replace'))
    return p.stdout.decode(errors='replace')

def tree():
    adb('shell', 'uiautomator', 'dump', '/sdcard/reto9-ui.xml')
    return ET.fromstring(adb('shell', 'cat', '/sdcard/reto9-ui.xml'))

def click(text=None, cls=None):
    for node in tree().iter('node'):
        if (text is None or node.get('text') == text) and (cls is None or node.get('class') == cls):
            x1,y1,x2,y2 = map(int,re.findall(r'\d+',node.get('bounds')))
            adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
            return
    raise AssertionError('Control no encontrado: '+str(text or cls))

def texts():
    return '\n'.join(n.get('text','') for n in tree().iter('node'))

def capture(name):
    adb('shell','screencap','-p','/sdcard/reto9-evidence.png')
    adb('pull','/sdcard/reto9-evidence.png',str(OUT / (name+'.png')))
    (OUT / (name+'.xml')).write_text(ET.tostring(tree(),encoding='unicode'),encoding='utf-8')

def check(label, condition):
    report.append(('PASS' if condition else 'FAIL')+' | '+label)
    (OUT/'functional-tests.txt').write_text('\n'.join(report),encoding='utf-8')
    return condition

def wait_results():
    for _ in range(20):
        current=texts()
        if 'Consultando' not in current:
            return current
        time.sleep(3)
    return texts()

def found_places(current):
    return 'lugares encontrados' in current or 'hospitales encontrados' in current

adb('shell','am','force-stop','co.edu.ucc.reto9')
adb('shell','am','start','-n','co.edu.ucc.reto9/.MainActivity')
time.sleep(2)
capture('01-inicio')
click('MI GPS')
time.sleep(1)
permission=texts()
check('Se solicita permiso de ubicación en tiempo de ejecución','permissioncontroller' in ET.tostring(tree(),encoding='unicode'))
capture('02-permiso-gps')
adb('shell','input','keyevent','4')
adb('shell','pm','grant','co.edu.ucc.reto9','android.permission.ACCESS_FINE_LOCATION')
adb('shell','pm','grant','co.edu.ucc.reto9','android.permission.ACCESS_COARSE_LOCATION')
adb('emu','geo','fix','-74.08175','4.60971')
click('MI GPS')
time.sleep(2)
adb('emu','geo','fix','-74.08175','4.60971')
time.sleep(3)
current=wait_results()
check('GPS simulado de Bogotá recibido','4.60971, -74.08175' in current)
check('Consulta real Overpass devuelve nombres',found_places(current))
capture('03-gps-lugares')
click(cls='android.widget.Spinner')
click('Hospitales')
current=wait_results()
check('Filtro hospitales consulta lugares reales',found_places(current))
capture('04-hospitales')
click('RADIO')
click(cls='android.widget.EditText')
adb('shell','input','keyevent','KEYCODE_MOVE_END')
for _ in range(8): adb('shell','input','keyevent','KEYCODE_DEL')
adb('shell','input','text','0')
click('GUARDAR')
check('Radio inválido mantiene abierto el diálogo','Radio de búsqueda' in texts())
capture('05-radio-invalido')
click(cls='android.widget.EditText')
adb('shell','input','keyevent','KEYCODE_DEL')
adb('shell','input','text','1.5')
click('GUARDAR')
time.sleep(1)
adb('shell','input','keyevent','4')
wait_results()
adb('shell','am','force-stop','co.edu.ucc.reto9')
adb('shell','am','start','-n','co.edu.ucc.reto9/.MainActivity')
time.sleep(2)
check('Radio 1.5 km persiste al reiniciar','Radio: 1.5 km' in texts())
capture('06-preferencias-persistentes')
click(cls='android.widget.EditText')
adb('shell','input','text','4.60971,-74.08175')
adb('shell','input','keyevent','4')
click('BUSCAR')
current=wait_results()
check('Origen por coordenadas interpretado correctamente','4.60971, -74.08175' in current)
check('Consulta Overpass desde coordenadas devuelve lugares',found_places(current))
capture('07-coordenadas')
print('\n'.join(report))
sys.exit(1 if any(line.startswith('FAIL') for line in report) else 0)
