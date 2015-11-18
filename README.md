# Muskel2

Muskel2 è una libreria Java per il calcolo distribuito che implementa le specifiche  Reactive Streams (http://www.reactive-streams.org/). 

Requisiti:
* Java 1.8+
* Maven (per la sola compilazione)

### Features

* Possibilità di creare programmi distribuiti in modo simile a come si creano gli Stream in Java 8 o con RxJava
* Soluzione decentralizzata ed estendibile (ogni nodo può essere esteso con servizi custom)
* I nodi server non devono possedere all'avvio il programma che si desidera eseguire
* Possibilità di eseguire parti oppure interi programmi completamente in remoto 
* Possibilità di richiedere l'esecuzione di una funzione remota in un sottoinsieme di nodi.


### Esempi di utilizzo

A partire da una lista di profili utente presente localmente si vuol effettuare la generazione della Thumbnail su uno specifico insieme di nodi "imageProcessorNodeGroup" e passare il risultati al gruppo di server responsabile per la loro memorizzazione "storeImageNodeGroup". In questo caso il programma non torna alcun controllo all'utente finale.
```java
MuskelProcessor.fromIterable(dao.getUserProfiles())
	          .executeOn(buildRemote("imageProcessorNodeGroup"))
		  .map(userProfile -> thumbnailService.generateThumbnail(userProfile))
		  .executeOn(buildRemote("storeImageNodeGroup"))
		  .subscribe(profileImage -> {			
					//Salva immagine 
				});
```

Oppure supponendo che il dao contenente la lista dei profili utenti si trovi su un server remoto, è possibile invocare l'operazione sul server ed ottenere il risultato in locale.
```java
List<Thumbnail> thumbnails = MuskelProcessor
   .executeOn(() -> MuskelProcessor.fromIterable(dao.getUserProfiles())
   .map(userProfile -> thumbnailService.generateThumbnail(userProfile)),buildRemote("profileNode"))
.toList().
toBlocking().
getFirst();
```

Inoltre le funzioni ``` map ``` e ``` flatMap ``` come quelle definite negli Stream di Java 8 possono essere eseguite sia in modo parallelo locale che remoto!

In questo caso la sola funzione map viene eseguita sul gruppo di server specifico
```java
MuskelProcessor.fromIterable(dao.getUserProfiles())
	  ....
 .map(userProfile -> thumbnailService.generateThumbnail(userProfile), buildRemote("nodeCalculator"))
		  ....
 .subscribe(profileImage -> {			
					//Salva immagine 
				});
```
