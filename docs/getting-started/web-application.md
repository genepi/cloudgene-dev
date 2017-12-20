# Running apps in the Web Application

The webservice displays a graphical userinterface for all installed applications. The webserver can be started with the following command:

```sh
cloudgene server
```

The webservice is available on http://localhost:8082. Please use username `admin` and password `admin1978` to login. You can use the `--port` flag to change the port from `8082` to `8085`:

```sh
cloudgene server --port 8085
```

*For production you should use the `cloudgene-daemon.sh` script. Learn [more]()*



## Connect it with a Hadoop cluster

Cloudgene needs a Hadoop cluster to execute MapReduce steps. Cloudgene uses the default configuration of your Hadoop Cluster. If it is installed on the Hadoop Namenode you can run it without additional flags. Otherwise you have to provide the url of your Hadoop cluster.

## Running on a local workstation using Docker

Start the server with the `--docker` flag:

```sh
cloudgene server --docker
```

Cloudgene starts automatically the needed docker container and executes your MapReduce steps on the cluster inside the container. We use an [image](https://github.com/seppinho/cdh5-hadoop-mrv1) from seppinho as our default image. You can use the `--image` flag if you want to use a custom docker image (e.g. a fork of our image with some special adaptations you need for your workflow):

```sh
cloudgene run server --docker --image myuser/my-image
```


## Running on a remote Hadoop cluster

Start the server with the `--host` flag to set the IP address of your remote Hadoop cluster:

```sh
cloudgene server --host <remote-ip>
```

Cloudgene executes your MapReduce steps on the remote cluster. You can use the `--user` flag to set the username which should be used to execute your job (e.g. it uses the HDFS directory of this user for all files):

```sh
cloudgene server --host <remote-ip> --user <remote-user>
```

## Reusing Docker containers

Cloudgene's built-in support for docker starts and stops the container each time you execute a workflow. During developement it is more convenient to reuse a running docker container. You have to start the container by hand:

```sh
docker run -it -h cloudgene -p 50030:50030 seppinho/cdh5-hadoop-mrv1:latest run-hadoop-initial.sh
```
When the container is ready, you see the following output:

```ansi
 * Started Hadoop datanode (hadoop-hdfs-datanode):
 * Started Hadoop namenode:
 * Started Hadoop secondarynamenode:
 * Started Hadoop jobtracker:
 * Started Hadoop tasktracker:
Congratulations! Cluster is running on 172.17.0.2
```

You can now use the provided address (e.g. 172.17.0.2) to run all Cloudgene workflows in the same container:
```sh
cloudgene server --host 172.17.0.2
```

You can also take advantage of all Hadoop web-interfaces to debug your job (e.g. 172.17.0.2:50030 and 172.17.0.2:50070).

Don't forget to use `docker ps` and `docker kill` to stop your containers. Please use `docker pull` to ensure you are using the latest image.