package br.com.sambatest.core.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

// Auto-generated Javadoc
/**
 * Classe utilitaria para de Bundle.
 *
 * @author luis.fernandez
 */
public class AppParametersUtil {

	/** The bundle. */
	private static ResourceBundle bundle;

	static{
		bundle = ResourceBundle.getBundle("application", new Locale("pt_br"));		
	}
	
	/**
	 * Recupera.
	 *
	 * @param chave the chave
	 * @param parametros the parametros
	 * @return the string
	 */
	public static String recupera(String chave, Object... parametros){
		return formata(bundle.getString(chave), parametros);
	}
	
	/**
	 * Formata.
	 *
	 * @param mensagem the mensagem
	 * @param parametros the parametros
	 * @return the string
	 */
	private static String formata(String mensagem, Object... parametros){
		return MessageFormat.format(mensagem, parametros); 
	}
	
}
