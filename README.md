# mqjms

### MQ server setup

dis qmgr connauth<br/>
qmname(QM1) connauth(SYSTEM.DEFAULT.AUTHINFO.IDPWOS)<br/>

dis authinfo(SYSTEM.DEFAULT.AUTHINFO.IDPWOS)<br/>
authinfo(SYSTEM.DEFAULT.AUTHINFO.IDPWOS) authtype(IDPWOS) ...<br/>

set authrec objtype(qmgr) principal('mqapp1') authadd(connect, inq)

define channel(DEV.HELLO) chltype(SVRCONN) trptype(TCP)<br/>
set chlauth(DEV.HELLO) type(usermap) clntuser('mqapp1') usersrc(map) mcauser('mqapp1') address('*')

define qlocal(DEV.Q1)<br/>
set authrec profile(DEV.Q1) objtype(queue) principal('mqapp1') authadd(put,get,inq,browse)

### MQ server SSL setup

alter channel(DEV.HELLO) chltype(SVRCONN) SSLCIPTH(ANY)
alter channel(DEV.HELLO) chltype(SVRCONN) SSLCAUTH(REQUIRED)

### MQ key store.

dis qmgr sslkeyr
sslkeyr(C:\ProgramData\IBM\MQ\qmgrs\qm1\ssl\key)

#### create keystore
runmqckm -keydb -create -type cms -db key -pw <password>
runmqckm -keydb -stashpw -db key.kdb -type cms -pw <password>

#### create queue manager keypair
runmqckm -cert -create -db key.kdb -stashed -label ibmwebspheremqqm1
    -dn "CN=QM1, OU=Infra, O=Szesto io, L=Los Angeles, ST=CA, C=US"
    -size 1024 -x509version 3 -expire 365 -sig_alg SHA384WithRSA

#### export queue manager certificate
runmqckm -cert -extract -db key.kdb -stashed -label ibmwebspheremqqm1
    -target ibmwebspheremqqm1cert.cer -format ascii

#### add trusted certificate to queue manager key store for client authentication
runmqckm -cert -add -db key.kdb -stashed -file cert.cer -trust enable

### jdk key store

#### generate keypair
keytool -genkeypair -alias alias -keyalg RSA -validity 365 -keystore keystore

#### export certificate from keystore
keytool -export -alias alias -keystore keystore -rfc -file cert.cer

#### import trusted certificate from queue manager
keytool -import -alias ibmwebspheremqqm1 -file cert.cer -keystore truststore

