package it.polito.tdp.metroparis.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.javadocmd.simplelatlng.LatLng;

import it.polito.tdp.metroparis.model.CoppieF;
import it.polito.tdp.metroparis.model.Fermata;
import it.polito.tdp.metroparis.model.Linea;

public class MetroDAO {

	public List<Fermata> readFermate() {

		final String sql = "SELECT id_fermata, nome, coordx, coordy FROM fermata ORDER BY nome ASC";
		List<Fermata> fermate = new ArrayList<Fermata>();

		try {
			Connection conn = DBConnect.getConnection();
			PreparedStatement st = conn.prepareStatement(sql);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				Fermata f = new Fermata(rs.getInt("id_Fermata"), rs.getString("nome"),
						new LatLng(rs.getDouble("coordx"), rs.getDouble("coordy")));
				fermate.add(f);
			}

			st.close();
			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Errore di connessione al Database.");
		}

		return fermate;
	}

	public List<Linea> readLinee() {
		final String sql = "SELECT id_linea, nome, velocita, intervallo FROM linea ORDER BY nome ASC";

		List<Linea> linee = new ArrayList<Linea>();

		try {
			Connection conn = DBConnect.getConnection();
			PreparedStatement st = conn.prepareStatement(sql);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				Linea f = new Linea(rs.getInt("id_linea"), rs.getString("nome"), rs.getDouble("velocita"),
						rs.getDouble("intervallo"));
				linee.add(f);
			}

			st.close();
			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Errore di connessione al Database.");
		}

		return linee;
	}

	public boolean isConnesse(Fermata partenza, Fermata arrivo) {

		final String sql = "SELECT COUNT(*) AS c "
				+ "FROM connessione "
				+ "WHERE id_stazP=? AND id_stazA=?";
		
		try {
			Connection conn = DBConnect.getConnection();
			PreparedStatement st = conn.prepareStatement(sql);
			
			//Devo settare i due parametri con il '?'
			st.setInt(1, partenza.getIdFermata());
			st.setInt(2, arrivo.getIdFermata());
			
			ResultSet rs = st.executeQuery();
			rs.first();  //quando la query restituisce un solo dato
			int c = rs.getInt("c");
			conn.close();
			
			return c!=0;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	public List<Fermata> trovaCollegate(Fermata partenza) {
		
		final String sql = "SELECT * "
				+ "FROM fermata "
				+ "WHERE id_fermata in (SELECT id_stazA "
				+ "							FROM connessione "
				+ "							WHERE id_stazP=? "
				+ "							GROUP BY id_stazA) "
				+ "ORDER BY nome";
		
		List<Fermata> listaFermate = new ArrayList<>();
		
		try {
			Connection conn = DBConnect.getConnection();
			PreparedStatement st = conn.prepareStatement(sql);
			st.setInt(1, partenza.getIdFermata());
			ResultSet rs = st.executeQuery();
			
			while(rs.next()) {
				Fermata f = new Fermata(rs.getInt("id_fermata"), rs.getString("nome"), new LatLng(rs.getDouble("coordX"), rs.getDouble("coordY")));
				listaFermate.add(f);
			}
			st.close();
			conn.close();
			return listaFermate;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

public List<Fermata> trovaIdCollegate(Fermata partenza, Map<Integer, Fermata> fermataIdMap) {
		
		final String sql = "SELECT id_stazA "
				+ "FROM connessione "
				+ "WHERE id_stazP=? "
				+ "GROUP BY id_stazA";
		
		List<Fermata> listaFermate = new ArrayList<>();
		
		try {
			Connection conn = DBConnect.getConnection();
			PreparedStatement st = conn.prepareStatement(sql);
			st.setInt(1, partenza.getIdFermata());
			ResultSet rs = st.executeQuery();
			
			while(rs.next()) {
				Integer idFermata = rs.getInt("id_stazA");
				listaFermate.add(fermataIdMap.get(idFermata));
			}
			st.close();
			conn.close();
			return listaFermate;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public List<CoppieF> getAllCoppie(Map<Integer, Fermata> fermateIdMap){
		final String sql = "SELECT distinct id_stazP, id_stazA "
				+ "FROM connessione";
		List<CoppieF> listaCoppieFermate = new ArrayList<>();
		
		try {
			Connection conn = DBConnect.getConnection();
			PreparedStatement st = conn.prepareStatement(sql);
			ResultSet rs = st.executeQuery();
			
			while(rs.next()) {
				//Creo l'oggetto coppia 
				CoppieF coppia = new CoppieF(fermateIdMap.get(rs.getInt("id_stazP")), fermateIdMap.get(rs.getInt("id_stazA")));
				listaCoppieFermate.add(coppia);
			}
			st.close();
			conn.close();
			return listaCoppieFermate;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	}

}
