package it.polito.tdp.metroparis.model;

import java.util.*;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import it.polito.tdp.metroparis.db.MetroDAO;

public class Model {

	private Graph<Fermata, DefaultEdge> grafo;
	private List<Fermata> listaFermate;
	private Map<Integer, Fermata> fermateIdMap; //posso passare al dao una mappa che dall'id mi fa arrivare all'oggetto fermata
	
	public void creaGrafo() {
		
		this.grafo = new SimpleGraph <Fermata, DefaultEdge>(DefaultEdge.class); //crea l'oggetto grafo
		
		//AGGIUNGI I VERTICI: sono le fermate nella lista del dao
		
		MetroDAO dao = new MetroDAO();
		this.listaFermate = dao.readFermate();
		Graphs.addAllVertices(this.grafo, this.listaFermate);
		
		fermateIdMap = new HashMap<>(); 
		for(Fermata f : this.listaFermate) {
			this.fermateIdMap.put(f.getIdFermata(), f); //mi creo una mappa oltre alla lista delle fermate
		}
		
		//AGGIUNGI GLI ARCHI
		
		//Metodo 1: considero tutti i potenziali archi
//		long tic = System.currentTimeMillis();
//		for(Fermata partenza : this.grafo.vertexSet()) { 
//			for(Fermata arrivo : this.grafo.vertexSet()) {//per ciascun vertice di partenza considero tutti i possibili vertici di arrivo
//				//mi chiedo se per ogni coppia di vertici partenza/arrivo c'è un arco
//				if(dao.isConnesse(partenza, arrivo)) {
//					this.grafo.addEdge(partenza, arrivo);
//					//se ho già inserito a,b e voglio inseribe b,a in caso di grafo non orientato non devo fare controlli aggiuntivi
//				}
//			}
//		}
//		long toc = System.currentTimeMillis();
//		System.out.println("Elapsed time: " + (toc-tic));
		
		//metodo 2: prendiamo una fermata alla volta e per ciascuna chiediamo quali sono le fermate adiacenti e poi le aggiungo come archi
		long tic1 = System.currentTimeMillis();
		for(Fermata partenza : this.grafo.vertexSet()) {
			List<Fermata> collegate = dao.trovaCollegate(partenza);  //dobbiamo restituire una lista di fermate ma la query restituisce delle connessioni dunque creiamo una map
			for(Fermata arrivo : collegate) {
				this.grafo.addEdge(partenza, arrivo);
			}
		}
		long toc1 = System.currentTimeMillis();
		System.out.println("Elapsed time: " + (toc1-tic1));
		
		//Metodo 2a: semplificazione query, dove data una fermata troviamo una lista di id connessi e poi in java utilizzando la fermateIdMap recuperiamo la fermata corrispondente
		//Semplificando la query si riduce il tempo di quasi la metà
		long tic2 = System.currentTimeMillis();
		for(Fermata partenza : this.grafo.vertexSet()) {
			List<Fermata> collegate = dao.trovaIdCollegate(partenza, fermateIdMap);  //dobbiamo restituire una lista di fermate ma la query restituisce delle connessioni dunque creiamo una map
			for(Fermata arrivo : collegate) {
				this.grafo.addEdge(partenza, arrivo);
			}
		}
		long toc2 = System.currentTimeMillis();
		System.out.println("Elapsed time: " + (toc2-tic2));
		
		//metodo 3: faccio una query per prendermi tutti gli edges MIGLIORI
		long tic3= System.currentTimeMillis();
		List<CoppieF> coppie = dao.getAllCoppie(fermateIdMap);
		for(CoppieF c : coppie) {
			this.grafo.addEdge(c.getPartenza(), c.getArrivo());
		}
		long toc3 = System.currentTimeMillis();
		System.out.println("Elapsed time: " + (toc3-tic3));
		
		System.out.println("Grafo creato con " + this.grafo.vertexSet().size() + " vertici e " + this.grafo.edgeSet().size() + " archi");
	}
	
	public List<Fermata> getAllFermate() {
		MetroDAO dao = new MetroDAO();
		return dao.readFermate();
	}
	
	public boolean isGrafoLoaded() { //mi dà l'info che effettivamente il grafo è stato creato
		return this.grafo.vertexSet().size()>0;
	}
	
	public List<Fermata> percorso(Fermata partenza, Fermata arrivo) { //determina il percorso minimo tra due fermate
//		1. VISITARE IL GRAFO PARTENDO DALLA STAZIONE DI PARTENZA, quindi fare una visita in ampiezza
//		del grafo che mi costruisce l'albero di visita
		BreadthFirstIterator <Fermata, DefaultEdge> visita =
			new BreadthFirstIterator<>(/*ha bisogno del grafo su cui iterare*/ this.grafo, 
					/*e del singolo o di un insieme di vertici di partenza*/ partenza);
		List<Fermata> raggiungibili = new ArrayList<Fermata>(); //questa lista la popolo via via con i risultati che mi dà l'iteratore
		while(visita.hasNext()) {
			Fermata f = visita.next();
			raggiungibili.add(f);
		}
//		System.out.println(raggiungibili);	
		
//		2. ANALISI DELL'ALBERO PER AVERE IL PERCORSO
		List<Fermata> percorso = new ArrayList<Fermata>();
//		Lavoro a ritroso perchè il percorso all'indietro è UNIVOCO in un albero, un vertice ha sempre e solo un predecessore, mentre può avere più successori
//		fino a che arrivo alla fermata di partenza che è l'unica a non avere un precedente
		Fermata corrente = arrivo;
		percorso.add(arrivo);
		DefaultEdge e = visita.getSpanningTreeEdge(corrente); //gli passo l'ultimo vertice che sto considerando e mi restituisce l'arco tramite il quale ci siamo arrivati
//		mettiamo in ciclo fin tanto che 'e' non risulta null
		while(e != null) {
			Fermata precedenteAllaCorrente = Graphs.getOppositeVertex(this.grafo, e, corrente);  //mi dà il vertice opposto sull'arco 'e' rispetto al vertice corrente
			percorso.add(0, precedenteAllaCorrente); //mi aggiunge in testa così ce l'ho ordinata, più efficiente LINKEDLIST
			corrente = precedenteAllaCorrente;
			e = visita.getSpanningTreeEdge(corrente);
		}
		return percorso;
	}
}
