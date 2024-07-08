package com.example.WebGoatJava.Controller;

import com.example.WebGoatJava.Dto.dtoProducto;
import com.example.WebGoatJava.Entity.Producto;
import com.example.WebGoatJava.Entity.Usuario;
import com.example.WebGoatJava.JWT.JwtUtil;
import com.example.WebGoatJava.Service.ProductoService;
import com.example.WebGoatJava.Service.UsuarioService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
//Mapear las direcciones web(get, post, put, delate)
@RequestMapping(value = "/producto")
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = {"Origin", "Content-Type", "Accept", "Authorization"})
public class ProductoController {
        // private final static Logger logger = LoggerFactory.getLogger();
        @Autowired
        ProductoService serviceProducto;
        @Autowired
        UsuarioService serviceUsuario;
        @Value("${fotodirectory}")
        private String ruta;
        public String downloadPath = "C:/Users/appse/OneDrive/Desktop/WebGoatJava/Backend/WebGoatJava/src/main/webapp/WEB-INF/jsp";
        List<String> extensionesPermitidas = Arrays.asList("jpg", "jpeg", "png");
        @GetMapping("/lista")
        //responder mediante entidades
        public ResponseEntity<List<Producto>> List() {
            List<Producto> list = serviceProducto.list();
            System.out.println("Peticion desde el front");
            return new ResponseEntity(list, HttpStatus.OK);
        }
    @GetMapping("/users")
    //responder mediante entidades
    public ResponseEntity<List<Usuario>> User() {
        List<Usuario> users = serviceUsuario.users();
        System.out.println("Peticion desde el front");
        return new ResponseEntity(users, HttpStatus.OK);
    }

        //responder mediante los detalles

        @GetMapping("/detail/{id}")
        public ResponseEntity<Producto> getById(@PathVariable("id") int id) {
            if (!serviceProducto.existsById(id)) {
                return new ResponseEntity(new Mensaje("El ID No existe"), HttpStatus.NOT_FOUND);
            }
            Producto producto = serviceProducto.getOne(id);
            return new ResponseEntity(producto, HttpStatus.OK);
        }

        @PostMapping(value = "/create", consumes = {"multipart/mixed", "multipart/form-data", "application/json", "text/plain", "application/x-www-form-urlencoded"})
        //crear un producto
        public ResponseEntity<?> create(@ModelAttribute dtoProducto dtoproducto, @RequestParam(name = "file", required = false) MultipartFile foto) {
            //verificar que la cadena no este en blanco
            if (StringUtils.isEmpty(dtoproducto.getServicio())) {
                return new ResponseEntity(new Mensaje("El servicio es obligatorio"), HttpStatus.BAD_REQUEST);
            }
            if (dtoproducto.getPrecio() < 50) {
                return new ResponseEntity(new Mensaje("El precio mínimo en de $50"), HttpStatus.BAD_REQUEST);
            }
            if (serviceProducto.existsByServicio(dtoproducto.getServicio()))
                return new ResponseEntity<>(new Mensaje("El servicio ya existe"), HttpStatus.BAD_REQUEST);
            try {

                String originalFileName = foto.getOriginalFilename();
                String extension = FilenameUtils.getExtension(originalFileName);
                if (!extensionesPermitidas.contains(extension.toLowerCase())) {
                    return new ResponseEntity(new Mensaje("Extensión de archivo no permitida."), HttpStatus.BAD_REQUEST);
                }
                // Genera un nombre único para el archivo
                String nuevoNombre = UUID.randomUUID().toString() + "." + extension;
                // Path completo para el nuevo archivo
                String filePath = ruta + "/" + nuevoNombre;
                // Guarda el archivo en la ruta
                File dest = new File(filePath);
                foto.transferTo(dest);

                Producto producto = new Producto(dtoproducto.getPrecio(), dtoproducto.getServicio(), dtoproducto.getCategoria(), dtoproducto.getDescripcion(), dtoproducto.getFoto(), dtoproducto.getUrl());
                producto.setFoto(nuevoNombre);
                serviceProducto.save(producto);
                if (producto.getUrl().isEmpty()) {
                    System.out.println("url: vacio");
                } else {

                    BufferedInputStream in = new BufferedInputStream(new URL(producto.getUrl()).openStream());
                    URL url = new URL(producto.getUrl());
                    // Crea un objeto Resource a partir de la URL
                    UrlResource resource = new UrlResource(url);
                    if (resource.exists() && resource.isReadable()) {
                        System.out.println("descarga");
                        System.out.println(downloadPath);
                        File dest2 = new File(downloadPath);
                        foto.transferTo(dest2);

                        return new ResponseEntity(new Mensaje("Producto creado"), HttpStatus.OK);

                    } else {
                        return ResponseEntity.notFound().build();
                    }
                }
                // logger.info("Producto creado");
                return new ResponseEntity(new Mensaje("Producto creado"), HttpStatus.OK);
            } catch (Exception e) {
                // logger.info("Producto no creado");
                return new ResponseEntity<>("Error al crear el producto " + e, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        @PutMapping("/update/{id}")
        public ResponseEntity<?> update(@RequestHeader("Authorization") String tokenActual,@PathVariable("id") int id, @ModelAttribute dtoProducto dtoproducto, @RequestParam(name = "file", required = false) MultipartFile foto) {

            if (!serviceProducto.existsById(id))
                return new ResponseEntity(new Mensaje("El ID No existe"), HttpStatus.NOT_FOUND);
            if (serviceProducto.existsByServicio(dtoproducto.getServicio()) && serviceProducto.getByServicio(dtoproducto.getServicio()).getId() != id)
                return new ResponseEntity<>(new Mensaje("El servicio ya existe"), HttpStatus.BAD_REQUEST);
            if (StringUtils.isEmpty(dtoproducto.getServicio()))
                return new ResponseEntity(new Mensaje("El servicio es obligatorio"), HttpStatus.BAD_REQUEST);
            if (dtoproducto.getPrecio() < 100)
                return new ResponseEntity(new Mensaje("El precio mínimo en de $100"), HttpStatus.BAD_REQUEST);
            try {
                String originalFileName = foto.getOriginalFilename();
                String extension = FilenameUtils.getExtension(originalFileName);
                if (!extensionesPermitidas.contains(extension.toLowerCase())) {
                    return new ResponseEntity(new Mensaje("Extensión de archivo no permitida."), HttpStatus.BAD_REQUEST);
                }
                // Genera un nombre único para el archivo
                String nuevoNombre = UUID.randomUUID().toString() + "." + extension;
                // Path completo para el nuevo archivo
                String filePath = ruta + "/" + nuevoNombre;
                // Guarda el archivo en la ruta
                File dest = new File(filePath);
                foto.transferTo(dest);

                Producto producto = serviceProducto.getOne(id);
                producto.setPrecio(dtoproducto.getPrecio());
                producto.setServicio(dtoproducto.getServicio());
                producto.setCategoria(dtoproducto.getCategoria());
                producto.setDescripcion(dtoproducto.getDescripcion());
                producto.setFoto(nuevoNombre);
                producto.setUrl(dtoproducto.getUrl());
                serviceProducto.update(producto);
                return new ResponseEntity(new Mensaje("Producto actualizado"), HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error al actualizar el producto " + e, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        @DeleteMapping("/delete/{id}")
        public ResponseEntity<?> delete(@RequestHeader("Authorization") String tokenActual, @PathVariable("id") int id) {
            String token = tokenActual.replace("Bearer ", "");
            String rolUsuario = JwtUtil.extractRol(token);
            if(!"ROL_ADMIN".equals(rolUsuario)){
                return new ResponseEntity(new Mensaje("No tienes permiso para eliminar productos"), HttpStatus.FORBIDDEN);
            }
            if (!serviceProducto.existsById(id))
                return new ResponseEntity(new Mensaje("El ID No existe"), HttpStatus.NOT_FOUND);
            serviceProducto.delete(id);
            return new ResponseEntity(new Mensaje("Se elimino el producto"), HttpStatus.OK);
        }
    }

