docker build -f Dockerfile-centos-master-kubernetes -t ptoceti/jenkins-master-centos-kubernetes:latest .
docker tag ptoceti/jenkins-master-centos-kubernetes:latest docker-internal-nexus3.supermicro.intra.ruedenfer37.fr/ptoceti/jenkins-master-centos-kubernetes:latest
docker push docker-internal-nexus3.supermicro.intra.ruedenfer37.fr/ptoceti/jenkins-master-centos-kubernetes:latest