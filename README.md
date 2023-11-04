# mqjms

### MQ server setup

#### MQ server without autentication
dis qmgr connauth
qmname(QM1) connauth()

For jms client, the value of asserted user is os account name of the client application.<br/>
For example, if 'simon' is logged on the client machine, the channel mca user is 'simon'.<br/>

Set mcauser on the svrconn channel.<br/>
Make sure channel mcauser is valid user id on qmgr operating system.<br/>
Note that we did not define any chlauth records.

define channel(DEV.HELLO) chltype(SVRCONN) trptype(TCP)<br/>
alter channel(DEV.HELLO) chltype(SVRCONN) mcauser('mqapp1')

#### MQ server with authentication
dis qmgr connauth<br/>
qmname(QM1) connauth(SYSTEM.DEFAULT.AUTHINFO.IDPWOS)<br/>

dis authinfo(SYSTEM.DEFAULT.AUTHINFO.IDPWOS)<br/>
authinfo(SYSTEM.DEFAULT.AUTHINFO.IDPWOS) authtype(IDPWOS) ...<br/>

define channel(DEV.HELLO) chltype(SVRCONN) trptype(TCP)<br/>
set chlauth(DEV.HELLO) type(usermap) clntuser('mqapp1') usersrc(map) mcauser('mqapp1') address('*')<br/>

#### connection authorization
set authrec objtype(qmgr) principal('mqapp1') authadd(connect, inq)<br/>

#### local queue
define qlocal(DEV.Q1)<br/>
set authrec profile(DEV.Q1) objtype(queue) principal('mqapp1') authadd(put,get,inq,browse)<br/>

### MQ server SSL setup

alter channel(DEV.HELLO) chltype(SVRCONN) SSLCIPTH(ANY)
alter channel(DEV.HELLO) chltype(SVRCONN) SSLCAUTH(REQUIRED)

### MQ key store.

dis qmgr sslkeyr<br/>
sslkeyr(C:\ProgramData\IBM\MQ\qmgrs\qm1\ssl\key)<br/>

#### create keystore
runmqckm -keydb -create -type cms -db key -pw password<br/>
runmqckm -keydb -stashpw -db key.kdb -type cms -pw password<br/>

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
