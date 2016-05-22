/**
 * 
 */
package br.com.sambatest.web.visao;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;

import lombok.Getter;
import lombok.Setter;

import org.primefaces.model.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import br.com.sambatest.core.model.Arquivo;
import br.com.sambatest.core.service.ArquivoService;

/**
 * @author leandro.pereira
 *
 */

@Controller
@RequestScoped
public class VideoController implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6652263107707848041L;

	@Autowired
	private ArquivoService service;
	
	@Getter
	@Setter
	private String teste = "teste";
	
	@Getter
	@Setter
	private Arquivo arquivo;
	
	@Getter
	@Setter
	private List<Arquivo> arquivosList;
	
	@Getter
	@Setter
	private UploadedFile file;
	
	@Getter
	@Setter
	private String keyConsulta;
	
	@Getter
	@Setter
	private Arquivo arquivoSelecionado;
	
	@PostConstruct
	public void init(){
		setArquivo(null);
		setArquivosList(new ArrayList<Arquivo>());
		setKeyConsulta(null);
		setArquivoSelecionado(null);
		setFile(null);
	}

	public void upload(){
		service.uploadFile(file);
		find();
		showMensagemInformacao("Operação realizada com sucesso!");
	}
	
	public void find(){
		setArquivosList(service.findAll(getKeyConsulta()));
	}
	
	public void excluir(Arquivo selectedArquivo){
		service.excluir(selectedArquivo);
		find();
		showMensagemInformacao("Operação realizada com sucesso!");
	}
	
	public void abrirArquivo(Arquivo selectedArquivo){
		setArquivoSelecionado(selectedArquivo);
		getArquivoSelecionado().setUrl(service.getUrlFile(arquivoSelecionado));
		System.out.println(arquivoSelecionado.getUrl());		
	}
	
	private void showMensagemInformacao(String msg){
		FacesContext.getCurrentInstance().addMessage("informacao",
				new FacesMessage(FacesMessage.SEVERITY_INFO, msg,""));
	}

}
