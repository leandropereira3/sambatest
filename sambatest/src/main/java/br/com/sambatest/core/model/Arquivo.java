/**
 * 
 */
package br.com.sambatest.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author leandro.pereira
 *
 */

@EqualsAndHashCode(of="key")
@ToString(of="key")
@Data
public class Arquivo {

	private String key;
	private Double size;
	private String url;
}
