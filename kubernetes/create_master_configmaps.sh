kubectl create configmap jenkins-master-customcacerts --from-file=customcacerts=../src/conf/resources/secrets/certs/custom_jre8_cacerts -n udd
kubectl create configmap ptoceti-authorities-ca-certs --from-file=ptoceti-ca.pem=../src/conf/resources/secrets/certs/ptoceti-authorities-ca.cert.pem -n udd