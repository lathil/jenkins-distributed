docker build -f Dockerfile-master -t ptoceti/jenkins-master:2.387.1-1 .
docker tag ptoceti/jenkins-master:2.387.1-1 docker-internal-nexus3.supermicro.intra.ruedenfer37.fr/ptoceti/jenkins-master:2.387.1-1
docker push docker-internal-nexus3.supermicro.intra.ruedenfer37.fr/ptoceti/jenkins-master:2.387.1-1