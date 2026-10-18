"""Create a local release key once. Never upload signing/ to a public repository."""
import os, pathlib, secrets, subprocess
root = pathlib.Path(__file__).resolve().parents[1]
folder = root / 'signing'
folder.mkdir(exist_ok=True)
key = folder / 'whale-release.jks'
if key.exists(): raise SystemExit('Signing key already exists. Keep it for future updates.')
password = secrets.token_urlsafe(32)
java = pathlib.Path(os.environ.get('JAVA_HOME', 'C:/Program Files/Android/Android Studio/jbr'))
env = dict(os.environ, WHALE_SIGN_PASSWORD=password)
subprocess.run([str(java/'bin/keytool.exe'), '-genkeypair', '-keystore', str(key), '-storetype', 'PKCS12', '-alias', 'whale', '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000', '-dname', 'CN=Whale Pet Android, O=Whale Pet', '-storepass:env', 'WHALE_SIGN_PASSWORD', '-keypass:env', 'WHALE_SIGN_PASSWORD', '-noprompt'],env=env,check=True,capture_output=True)
(folder/'signing.properties').write_text('storeFile=signing/whale-release.jks\nkeyAlias=whale\nstorePassword='+password+'\nkeyPassword='+password+'\n',encoding='ascii')
print('Local release signing key created. Back up signing/ privately.')
