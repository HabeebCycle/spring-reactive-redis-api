# Spring Boot Reactive API service with Reactive Redis as DB
DEMO: A user-service API using spring webflux and reactive-redis
### Service and ports

```
name: user-service
port: 8086
docker-port: 8080

name: redis-db
port: 6379
```
Clone the project using git clone and CD into the root folder

On PC
Start Redis on port 6379 on localhost and run
```
mvn spring-boot:run
```

On Docker
You can either use the Maven build-image, use the Google jib-maven plugin or use docker compose file attached.

Maven build-image
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-maven-plugin</artifactId>
</plugin>
```
then run
```
./mvnw spring-boot:build-image
```
To push to registry add and edit 
```xml
<!-- docs.spring.io/spring-boot/docs/current/maven-plugin/reference/htmlsingle/#buid-image-examples-->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <image>
            <name>docker.io/library/${project.build.finalName}</name>
            <publish>true</publish>
        </image>
        <docker>
            <publishRegistry>
                <username>user</username>
                <password>secret</password>
                <url>https://docker.io/v1/</url>
                <email>user@example.com</email>
            </publishRegistry>
        </docker>
    </configuration>
</plugin>
```
To use the Google jib-maven plugin in the project
```
<dependency>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>2.0.0</version>
</dependency>
```
To build locally
```
mvn compile jib:build
```
To build and push to registry
```
mvn compile jib:build -Djib.to.auth.username={REGISTRY_USERNAME} -Djib.to.auth.password={REGISTRY_PASSWORD}
```
Locally run
```
docker-compose -d up
OR
docker run --rm -p 6379:6379 redis -e redis-server --requirepass ${YOUR_PASS} --name redis-db
#docker run --rm -p 8080:8080 user-service --name user-service
```


## Deployment to kubernetes

#### If you are using Play with K8s playground, follow these instructions to create a cluster (https://labs.play-with-k8s.com/)

You can bootstrap a cluster as follows:

1. Initializes cluster master node:

kubeadm init --apiserver-advertise-address $(hostname -i)


2. Initialize cluster networking:

kubectl apply -n kube-system -f \
"https://cloud.weave.works/k8s/net?k8s-version=$(kubectl version | base64 |tr -d '\n')"


3. (Optional) Create an nginx deployment:

kubectl apply -f https://raw.githubusercontent.com/kubernetes/website/master/content/en/examples/application/nginx-app.yaml

```
--- creating redis deployment
$ kubectl create deployment redis-db --image=redis:alpine --dry-run -o=yaml >> redis-db-deployment.yaml
--- creating user-service-deployment.yaml
$ kubectl create deployment user-service --image=${YOUR_IMAGE} --dry-run -o=yaml >> user-service-deployment.yaml
$ echo --- >> user-service-deployment.yaml
$ kubectl create service clusterip k8s-user-service --tcp=8080:8080 --dry-run -o=yaml >> user-service-service.yaml
```

To view the file created, use 'cat' command
To edit the file, use 'vi' command follow by 'i' and when done, hit escape key follow by ':wq' and press enter key

Example:
```
cat user-service-deployment.yaml
vi user-service-deployment.yaml
press 'i' key, make changes and press 'Esc' key.
Type ':wq' and press 'Enter' key
```

Deploying the app to Kubernetes cluster
```
$ kubectl apply -f redis-db-deployment.yaml
$ kubectl apply -f user-service-deployment.yaml
$ kubectl apply -f user-service-service.yaml

$ kubectl get all     #Shows all pods, nodes, deployments and services running
```