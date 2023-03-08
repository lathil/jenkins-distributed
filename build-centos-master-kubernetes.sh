docker build -f Dockerfile-centos-master-solo-kubernetes -t ptoceti/jenkins-master-centos-kubernetes:2.204.6 .
docker tag ptoceti/jenkins-master-centos-kubernetes:2.204.6 docker-internal-nexus3.supermicro.intra.ruedenfer37.fr/ptoceti/jenkins-master-centos-kubernetes:2.204.6
docker push docker-internal-nexus3.supermicro.intra.ruedenfer37.fr/ptoceti/jenkins-master-centos-kubernetes:2.204.6