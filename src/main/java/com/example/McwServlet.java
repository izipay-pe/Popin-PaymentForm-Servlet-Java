package com.example;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Map;
import java.util.HashMap;

@WebServlet({"/", "/checkout", "/result"})
public class McwServlet extends HttpServlet {
    
    // Componentes principales para la plantilla, las propiedades y el controlador
    private TemplateEngine templateEngine;
    private McwProperties properties;
    private McwController mcwController;

    @Override
    public void init() throws ServletException {
	// Configuración para las plantillas Thymeleaf
        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(getServletContext());
        templateResolver.setPrefix("/WEB-INF/templates/");
        templateResolver.setSuffix(".html");
	templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setTemplateMode("HTML");

	// Iniciando Thymeleaf
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);

	// Iniciando las propiedades y el controlador
	properties = new McwProperties();
	mcwController = new McwController();
    }

    /**
     * @@ Manejo de rutas GET @@
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
	// Obtiene la ruta solicitada
        String path = request.getServletPath();
	// Creación del contexto para pasar los valores para la plantilla
        WebContext context = new WebContext(request, response, getServletContext());
	
	// Generar orderId
	String orderId = mcwController.generarOrderId();
        
	// Procesamiento según la ruta solicitada
	switch (path) {
	    // Renderiza la plantilla 'index' al solicitar la ruta raíz, checkout y result
            case "/":
		// Agregando el orderId al contexto
		context.setVariable("orderId", orderId);
		// Renderizando el template y enviando los datos agregados al contexto
                templateEngine.process("index", context, response.getWriter());
                break;
            case "/checkout":
                templateEngine.process("index", context, response.getWriter());
                break;
            case "/result":
                templateEngine.process("index", context, response.getWriter());
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                break;
        }
    }

    /**
     * @@ Manejo de rutas POST @@
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
	// Obtiene la ruta solicitada
        String path = request.getServletPath();
	// Creación del contexto para pasar los valores para la plantilla
        WebContext context = new WebContext(request, response, getServletContext());
	
	// Definición de las variables a usar
	String krHash = null;
    	String krHashAlgorithm = null;
    	String krAnswerType = null;
    	String krAnswer = null;
    	String krHashKey = null;
	String orderStatus = null;
	String orderId = null;
	JSONObject jsonResponse = null;	
	
        switch (path) {
            case "/checkout":
		// Procesando datos POST enviados de la ruta raíz y almacenándolos en un Map
		
		Map<String, String> parameters = new HashMap<>();
            	for (String param : new String[]{"firstName", "lastName", "email", "phoneNumber", "identityType", 
                                              "identityCode", "address", "country", "state", "city", "zipCode", 
                                              "orderId", "amount", "currency"}) {
                	parameters.put(param, request.getParameter(param));
            	}
		
		//Obtener PublicKey
		String publicKey = properties.getProperty("publicKey");

		//Obtenemos el FormToken generado
		String formToken = mcwController.generarToken(parameters);
		
		//Agrerar el token en el contexto para ser usado en el template
		context.setVariable("formToken", formToken);
		context.setVariable("publicKey", publicKey);

		// Renderizando el template y enviando los datos agregados al contexto
		templateEngine.process("checkout", context, response.getWriter());	

		break;		

            case "/result":
		// Asignando los valores de la respuesta de Izipay en las variables
		krHash = request.getParameter("kr-hash");
        	krHashAlgorithm = request.getParameter("kr-hash-algorithm");
        	krAnswerType = request.getParameter("kr-answer-type");
        	krAnswer = request.getParameter("kr-answer");
        	krHashKey = request.getParameter("kr-hash-key");
		
		// Almacenamos los datos del kr-answer
		jsonResponse = new JSONObject(krAnswer);
		// Convertimos el valor del JSON obtenido a 'pretty print' para su visualización en el template
		String pJson = jsonResponse.toString(4);

		// Almacenamos los datos del pago en las variables
		orderStatus = jsonResponse.getString("orderStatus");
        	int orderTotalAmount = jsonResponse.getJSONObject("orderDetails").getInt("orderTotalAmount");
        	orderId = jsonResponse.getJSONObject("orderDetails").getString("orderId");
		String currency = jsonResponse.getJSONObject("orderDetails").getString("orderCurrency");

		// Formatear el valor de 'amount' de centimos a decimales
    		double orderAmountdouble = (double) orderTotalAmount / 100;
    		String orderAmount = String.format("%.02f", orderAmountdouble);
    

		// Válida que la data POST que obtiene /result tenga los valores esperados
		if (krHash != null && krHashAlgorithm != null && krAnswerType != null && krAnswer != null && krHashKey != null) {
			// Válida que la respuesta sea íntegra comprando el hash recibido en el 'kr-hash' con el generado con el 'kr-answer'
			boolean isValidKey = mcwController.checkHash(krHash, krHashKey, krAnswer);
			
			// Procesa la condicional si la firma es correcta
			if (isValidKey) {
			// Establece variables en el contexto para la plantilla			
			context.setVariable("krHash", krHash);
                	context.setVariable("krHashAlgorithm", krHashAlgorithm);
                	context.setVariable("krAnswerType", krAnswerType);
                 	context.setVariable("krAnswer", krAnswer);
                	context.setVariable("krHashKey", krHashKey);
			context.setVariable("pJson", pJson);
			context.setVariable("orderStatus", orderStatus);
			context.setVariable("orderTotalAmount", orderAmount);
			context.setVariable("orderId", orderId);
			context.setVariable("currency", currency);
			
			// Renderizando el template y enviando los datos agregados al contexto
			templateEngine.process("result", context, response.getWriter());
			
			break;

			} else {
				break;
			}

		} else {
        		break;
		}

	    
	    case "/ipn":
		// Asignando los valores de la respuesta IPN en las variables
		krHash = request.getParameter("kr-hash");
        	krAnswer = request.getParameter("kr-answer");
        	krHashKey = request.getParameter("kr-hash-key");
		
		// Procesa la respuesta del pago
		jsonResponse = new JSONObject(krAnswer);
		
		// Ejemplo de extracción de datos
		JSONArray transactionsArray = jsonResponse.getJSONArray("transactions");
		JSONObject transactions = transactionsArray.getJSONObject(0);
		
		// Verifica el orderStatus PAID
		orderStatus = jsonResponse.getString("orderStatus");
		orderId = jsonResponse.getJSONObject("orderDetails").getString("orderId");
		String uuid = transactions.getString("uuid");

		// Válida que la respuesta sea íntegra comprando el hash recibido en el 'kr-hash' con el generado con el 'kr-answer'
		boolean isValidKey = mcwController.checkHash(krHash, krHashKey, krAnswer);
		
		// Procesa la condicional si la firma es correcta
		if (isValidKey) {
			// Imprimiendo en el log el Order Status
			System.out.println("OK! Order Status is " + orderStatus);
		} else {
			System.out.println("Notification Error");
		}

		break;
	    
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                break;
        }
    }
}

