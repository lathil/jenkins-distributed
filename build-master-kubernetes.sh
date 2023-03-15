docker build -f Dockerfile-master-solo-kubernetes -t ptoceti/jenkins-master-kubernetes:2.387.1-1 .
docker tag ptoceti/jenkins-master-kubernetes:2.387.1-1 docker-internal-nexus3.supermicro.intra.ruedenfer37.fr/ptoceti/jenkins-master-kubernetes:2.387.1-1
docker push docker-internal-nexus3.supermicro.intra.ruedenfer37.fr/ptoceti/jenkins-master-kubernetes:2.387.1-1