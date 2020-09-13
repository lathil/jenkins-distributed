cp jre8-cacerts custom_jre8_cacerts \
&& keytool -keystore custom_jre8_cacerts -storepass changeit -importcert -alias supermicro -file server-private.cert.pem -noprompt -trustcacerts