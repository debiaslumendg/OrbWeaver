# OrbWeaver
Segundo Proyecto de Sistemas de Operación II


## Pasos para correr los .jar

Estan en la carpeta out/artifacts/

los que terminan en _server son servidores.


### Para correr un .jar es java -jar <nombrejar>


$> java -jar wordcount.jar <lista de archivos>  [--hosts|-hs <número>] [--ports|-ps <número>]
* lista de archivos : Obligatorio, lista de rutas a archivos.
* --hosts  | -hs  : Address del coordinador (Scheduler) , opcional se usará el del archivo de configuración
* --ports  | -ps  : Puerto del coordinador (Scheduler), opcional se usará el del archivo de configuración

Ejemplo(con scheduler corriendo y con un caché generado ya):

$> java -jar wordcount.jar texto.txt

$> java -jar esprimo.jar <NUMBER>  [--hosts|-hs <NUMBER>] [--ports|-ps <NUMBER>]
*
*      NUMBER   : Obligatorio, numero a verificar si es primo
*      --hosts  | -hs  : Address del coordinador (Scheduler) , opcional se usará el de un archivo de configuración
*      --ports  | -ps  : Puerto del coordinador (Scheduler), opcional se usará el de un archivo de configuración

Ejemplo(con scheduler corriendo y con un caché generado ya):

java -jar esprimo.jar 1236


$> sortArray <ARRAY>  [--hosts|-hs <NUMBER>] [--ports|-ps <NUMBER>]
*
*      ARRAY   : Obligatorio, arreglo a ordenar
*      --hosts  | -hs  : Address del coordinador (Scheduler) , opcional se usará el de un archivo de configuración
*      --ports  | -ps  : Puerto del coordinador (Scheduler), opcional se usará el de un archivo de configuración


Ejemplo(con scheduler corriendo y con un caché generado ya):

java -jar sortarray.jar [4,3,2,1]

#### Nota de los caches:

Los clientes utilizan archivos locales para guardar la lista de miembros del grupo y la dirección del scheduler.
Archivos members.txt y scheduler.txt
Estos archivos pueden estar ya creados o no. Para generar unos nuevos o actualizarlos se debe pasar una dirección válida del scheduler al Cliente con los argumentos -hs <ip> y (--port <numero> ,opcional). 

Ejemplo:

$> java -jar sortarray.jar [4,3,2,1] -hs 127.231.231.123

Esto hace que se creen/regeneren los archivos members.txt y scheduler.txt.

Ya con esto se puede ejecutar los clientes sin necesidad de introducir quién es el scheduler.

$> java -jar sortarray.jar [4,3,2,1]

Si el scheduler falla, se utiliza el archivo members.txt para preguntar quien es el scheduler, notar que si se detecta en esta pregunta que un miembro falla se actualizará el archivo members.txt. 
El archivo scheduler.txt siempre se sobreescribe con la dirección del scheduler actual con la que se comunicó el cliente.



