package com.example.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.dao.CartRepository;
import com.example.dao.CategoryRepository;
import com.example.dao.OrderItemRepository;
import com.example.dao.ProductRepository;
import com.example.dao.WishlistRepository;
import com.example.dto.ProductDTO;
import com.example.dto.ProductResponseDTO;
import com.example.entity.Category;
import com.example.entity.Product;
import com.example.exception.ResourceNotFoundException;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private final CartRepository cartRepository;
    private final OrderItemRepository orderItemRepository;
    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final S3Service s3Service;

    public ProductService(ProductRepository productRepository,
                         CategoryRepository categoryRepository,
                         CartRepository cartRepository,
                         OrderItemRepository orderItemRepository,
                         S3Service s3Service,
                         WishlistRepository wishlistRepository) {
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.orderItemRepository = orderItemRepository;
        this.categoryRepository = categoryRepository;
        this.s3Service = s3Service;
        this.wishlistRepository = wishlistRepository;
    }

    @Transactional
    public ProductResponseDTO createProduct(ProductDTO productDTO) {
        logger.info("Creating new product: {}", productDTO.getName());
        
        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> {
                    logger.error("Category not found with id: {}", productDTO.getCategoryId());
                    return new ResourceNotFoundException("Category not found with id: " + productDTO.getCategoryId());
                });

        if (productDTO.getSubCategory() == null || productDTO.getSubCategory().isEmpty()) {
            throw new IllegalArgumentException("Subcategory is required");
        }

        if (!category.getSubCategories().contains(productDTO.getSubCategory())) {
            throw new IllegalArgumentException("Selected subcategory doesn't belong to the chosen category");
        }

        Product product = new Product();
        mapDtoToEntity(productDTO, product, category);
        
        // Save product first to get an ID
        Product savedProduct = productRepository.save(product);
        logger.debug("Product saved with ID: {}", savedProduct.getId());
        
        // Now handle image uploads with the folder structure
        List<String> imageUrls = handleImageUploads(productDTO.getImages(), savedProduct);
        
        if (!imageUrls.isEmpty()) {
            updateProductImages(savedProduct, imageUrls);
            savedProduct = productRepository.save(savedProduct);
            logger.info("Product created with {} images", imageUrls.size());
        } else {
            logger.info("Product created without images");
        }
        
        return mapEntityToResponseDto(savedProduct);
    }

    public ProductResponseDTO getProductById(String id) {
        logger.debug("Fetching product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Product not found with ID: {}", id);
                    return new ResourceNotFoundException("Product not found with id: " + id);
                });
        return mapEntityToResponseDto(product);
    }

    public ProductResponseDTO getProductById(String id, Long userId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        ProductResponseDTO dto = mapEntityToResponseDto(product);
        if (userId != null) {
            boolean inWishlist = wishlistRepository.existsByUserIdAndProductId(userId, id);
            dto.setInWishlist(inWishlist);
        }
        return dto;
    }

    public List<ProductResponseDTO> getAllProducts() {
        logger.debug("Fetching all products");
        List<Product> products = productRepository.findAll();
        logger.info("Found {} products", products.size());
        return products.stream()
                .map(this::mapEntityToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponseDTO updateProduct(String id, ProductDTO productDTO) {
        logger.info("Updating product with ID: {}", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (productDTO.getName() != null) {
            product.setName(productDTO.getName());
        }
        
        if (productDTO.getDescription() != null) {
            product.setDescription(productDTO.getDescription());
        }
        
        if (productDTO.getPrice() != null) {
            product.setPrice(productDTO.getPrice());
        }
        
        if (productDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            
            if (productDTO.getSubCategory() != null || !product.getCategory().getId().equals(productDTO.getCategoryId())) {
                String subCategoryToCheck = productDTO.getSubCategory() != null ? 
                    productDTO.getSubCategory() : product.getSubCategory();
                
                if (subCategoryToCheck == null || subCategoryToCheck.isEmpty()) {
                    throw new IllegalArgumentException("Subcategory is required");
                }
                
                if (!category.getSubCategories().contains(subCategoryToCheck)) {
                    throw new IllegalArgumentException("Selected subcategory doesn't belong to the chosen category");
                }
            }
            
            product.setCategory(category);
        }
        
        if (productDTO.getSubCategory() != null) {
            if (!product.getCategory().getSubCategories().contains(productDTO.getSubCategory())) {
                throw new IllegalArgumentException("Selected subcategory doesn't belong to the product's category");
            }
            product.setSubCategory(productDTO.getSubCategory());
        }
        
        if (productDTO.getQuantity() != null) {
            product.setQuantity(productDTO.getQuantity());
        }
        
        if (productDTO.getQuantityType() != null) {
            product.setQuantityType(productDTO.getQuantityType());
        }
        
        if (productDTO.getStatus() != null) {
            product.setStatus(productDTO.getStatus());
        }

        if (productDTO.getExpiryDate() != null) {
            product.setExpiryDate(productDTO.getExpiryDate());
        }

        if (productDTO.getImages() != null && !productDTO.getImages().isEmpty()) {
            deleteProductImages(product);
            List<String> imageUrls = handleImageUploads(productDTO.getImages(), product);
            updateProductImages(product, imageUrls);
        }

        Product updatedProduct = productRepository.save(product);
        return mapEntityToResponseDto(updatedProduct);
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> deleteProduct(String id) {
        logger.info("Deleting product with ID: {}", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Product not found with ID: {}", id);
                    return new ResourceNotFoundException("Product not found with id: " + id);
                });

        wishlistRepository.deleteByProductId(id);
        cartRepository.deleteByProductId(id);
        orderItemRepository.deleteByProductId(id);
        deleteProductImages(product);
        productRepository.delete(product);
        
        logger.info("Product with ID: {} deleted successfully", id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Product deleted successfully");
        response.put("productId", id);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    public List<ProductResponseDTO> getProductsByCategory(Integer categoryId) {
        logger.debug("Fetching products for category ID: {}", categoryId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    logger.error("Category not found with ID: {}", categoryId);
                    return new ResourceNotFoundException("Category not found with id: " + categoryId);
                });
        
        List<Product> products = productRepository.findByCategory(category);
        logger.info("Found {} products for category ID: {}", products.size(), categoryId);
        return products.stream()
                .map(this::mapEntityToResponseDto)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDTO> getProductsBySubCategory(String subCategory) {
        logger.debug("Fetching products for subcategory: {}", subCategory);
        List<Product> products = productRepository.findBySubCategory(subCategory);
        return products.stream()
                .map(this::mapEntityToResponseDto)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDTO> getProductsByCategoryAndSubCategory(Integer categoryId, String subCategory) {
        logger.debug("Fetching products for category ID: {} and subcategory: {}", categoryId, subCategory);
        List<Product> products = productRepository.findByCategoryAndSubCategory(categoryId, subCategory);
        return products.stream()
                .map(this::mapEntityToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ProductResponseDTO> bulkCreateProducts(MultipartFile zipFile) {
        logger.info("Starting bulk product creation from zip file");
        
        Map<String, byte[]> imageFiles = new HashMap<>();
        List<ProductDTO> productDTOs = new ArrayList<>();
        
        try (ZipInputStream zipInputStream = new ZipInputStream(zipFile.getInputStream())) {
            Map<String, byte[]> zipEntries = new HashMap<>();
            ZipEntry entry;
            
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                byte[] content = zipInputStream.readAllBytes();
                zipEntries.put(entryName, content);
                zipEntries.put(normalizePath(entryName), content);
                zipInputStream.closeEntry();
            }
            
            for (Map.Entry<String, byte[]> zipEntry : zipEntries.entrySet()) {
                String entryName = zipEntry.getKey();
                byte[] content = zipEntry.getValue();
                
                if (entryName.endsWith(".xlsx") || entryName.endsWith(".xls")) {
                    logger.debug("Processing Excel file: {}", entryName);
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(content)) {
                        productDTOs = parseExcelFile(bis, zipEntries);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error processing zip file: {}", e.getMessage());
            throw new RuntimeException("Failed to process zip file", e);
        }
        
        return processProducts(productDTOs);
    }

    // Helper methods
    private String buildImageFolderPath(Product product) {
        return String.format("products/%s/%s/%s/", 
            product.getCategory().getName().toLowerCase().replace(" ", "-"),
            product.getSubCategory().toLowerCase().replace(" ", "-"),
            product.getId());
    }

    private List<String> handleImageUploads(List<MultipartFile> images, Product product) {
        List<String> imageUrls = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            logger.debug("Processing {} images for upload", images.size());
            String folderPath = buildImageFolderPath(product);
            
            for (MultipartFile file : images) {
                try {
                    String fileKey = s3Service.uploadFile(file, folderPath);
                    String fileUrl = s3Service.getFileUrl(fileKey);
                    imageUrls.add(fileUrl);
                    logger.trace("Successfully uploaded image: {}", file.getOriginalFilename());
                } catch (Exception e) {
                    logger.error("Failed to upload image: {}", e.getMessage());
                    throw new RuntimeException("Failed to upload image: " + e.getMessage());
                }
            }
        }
        return imageUrls;
    }

    private void deleteProductImages(Product product) {
        logger.debug("Deleting images for product ID: {}", product.getId());
        String folderPath = buildImageFolderPath(product);
        
        if (product.getImage1() != null) {
            String key1 = extractKeyFromUrl(product.getImage1());
            if (s3Service.fileExists(key1)) {
                s3Service.deleteFile(key1);
            }
        }
        if (product.getImage2() != null) {
            String key2 = extractKeyFromUrl(product.getImage2());
            if (s3Service.fileExists(key2)) {
                s3Service.deleteFile(key2);
            }
        }
        if (product.getImage3() != null) {
            String key3 = extractKeyFromUrl(product.getImage3());
            if (s3Service.fileExists(key3)) {
                s3Service.deleteFile(key3);
            }
        }
        if (product.getImage4() != null) {
            String key4 = extractKeyFromUrl(product.getImage4());
            if (s3Service.fileExists(key4)) {
                s3Service.deleteFile(key4);
            }
        }
        if (product.getImage5() != null) {
            String key5 = extractKeyFromUrl(product.getImage5());
            if (s3Service.fileExists(key5)) {
                s3Service.deleteFile(key5);
            }
        }
    }

    private void updateProductImages(Product product, List<String> imageUrls) {
        product.setImage1(imageUrls.size() > 0 ? imageUrls.get(0) : null);
        product.setImage2(imageUrls.size() > 1 ? imageUrls.get(1) : null);
        product.setImage3(imageUrls.size() > 2 ? imageUrls.get(2) : null);
        product.setImage4(imageUrls.size() > 3 ? imageUrls.get(3) : null);
        product.setImage5(imageUrls.size() > 4 ? imageUrls.get(4) : null);
    }

    private String extractKeyFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    private void mapDtoToEntity(ProductDTO dto, Product entity, Category category) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setPrice(dto.getPrice());
        entity.setCategory(category);
        entity.setSubCategory(dto.getSubCategory());
        entity.setQuantity(dto.getQuantity());
        entity.setQuantityType(dto.getQuantityType());
        entity.setStatus(dto.getStatus());
        entity.setExpiryDate(dto.getExpiryDate());
    }

    private ProductResponseDTO mapEntityToResponseDto(Product entity) {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setPrice(entity.getPrice());
        
        // Calculate discounted price if discount exists for subcategory
        Category category = entity.getCategory();
        if (category != null && category.getSubcategoryDiscounts() != null) {
            Double discountPercentage = category.getSubcategoryDiscounts().get(entity.getSubCategory());
            if (discountPercentage != null && discountPercentage > 0) {
                BigDecimal discount = entity.getPrice().multiply(
                    BigDecimal.valueOf(discountPercentage / 100));
                dto.setDiscountedPrice(entity.getPrice().subtract(discount));
                dto.setDiscountPercentage(discountPercentage);
            }
        }
        
        dto.setCategoryId(entity.getCategory().getId());
        dto.setCategoryName(entity.getCategory().getName());
        dto.setSubCategory(entity.getSubCategory());
        dto.setQuantity(entity.getQuantity());
        dto.setQuantityType(entity.getQuantityType());
        dto.setStatus(entity.getStatus());
        dto.setExpiryDate(entity.getExpiryDate());
        
        List<String> imageUrls = new ArrayList<>();
        if (entity.getImage1() != null) imageUrls.add(entity.getImage1());
        if (entity.getImage2() != null) imageUrls.add(entity.getImage2());
        if (entity.getImage3() != null) imageUrls.add(entity.getImage3());
        if (entity.getImage4() != null) imageUrls.add(entity.getImage4());
        if (entity.getImage5() != null) imageUrls.add(entity.getImage5());
        dto.setImageUrls(imageUrls);
        
        return dto;
    }

    private List<ProductDTO> parseExcelFile(InputStream inputStream, Map<String, byte[]> imageFiles) throws IOException {
        List<ProductDTO> productDTOs = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            
            // Skip header row if exists
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }
            
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                ProductDTO dto = new ProductDTO();
                
                try {
                    // Required fields
                    dto.setName(getStringCellValue(row, 0));
                    dto.setDescription(getStringCellValue(row, 1));
                    dto.setPrice(parseBigDecimalCell(row, 2));
                    dto.setCategoryId(parseIntegerCell(row, 3));
                    dto.setSubCategory(getStringCellValue(row, 4));
                    dto.setQuantity(parseLongCell(row, 5));
                    dto.setQuantityType(getStringCellValue(row, 6));
                    dto.setStatus(getStringCellValue(row, 7));
                    
                    // Optional fields
                    dto.setExpiryDate(parseDateCell(row, 9));
                    
                    // Image handling
                    String imageRefs = getStringCellValue(row, 8);
                    if (imageRefs != null && !imageRefs.isEmpty()) {
                        List<MultipartFile> images = new ArrayList<>();
                        for (String ref : imageRefs.split(",")) {
                            ref = ref.trim();
                            byte[] imageBytes = imageFiles.get(ref);
                            
                            if (imageBytes != null) {
                                images.add(new InMemoryMultipartFile(
                                    ref,
                                    ref,
                                    "image/" + getFileExtension(ref),
                                    imageBytes));
                            }
                        }
                        dto.setImages(images.isEmpty() ? null : images);
                    }
                    
                    productDTOs.add(dto);
                    
                } catch (Exception e) {
                    logger.error("Error parsing row {}: {}", row.getRowNum() + 1, e.getMessage());
                    throw new IOException("Failed to parse row " + (row.getRowNum() + 1) + ": " + e.getMessage(), e);
                }
            }
        }
        
        logger.info("Successfully parsed {} products from Excel", productDTOs.size());
        return productDTOs;
    }

    private List<ProductResponseDTO> processProducts(List<ProductDTO> productDTOs) {
        List<ProductResponseDTO> responseDTOs = new ArrayList<>();
        
        for (ProductDTO dto : productDTOs) {
            try {
                Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId()));

                if (dto.getSubCategory() == null || dto.getSubCategory().isEmpty()) {
                    throw new IllegalArgumentException("Subcategory is required for product: " + dto.getName());
                }

                if (!category.getSubCategories().contains(dto.getSubCategory())) {
                    throw new IllegalArgumentException("Subcategory " + dto.getSubCategory() + 
                        " doesn't belong to category " + category.getName() + " for product: " + dto.getName());
                }

                Optional<Product> existingProductOpt = productRepository.findByName(dto.getName());
                
                if (existingProductOpt.isPresent()) {
                    Product existingProduct = existingProductOpt.get();
                    if (isProductModified(existingProduct, dto)) {
                        Product updatedProduct = updateProductFromDTO(existingProduct, dto);
                        Product savedProduct = productRepository.save(updatedProduct);
                        responseDTOs.add(mapEntityToResponseDto(savedProduct));
                    } else {
                        responseDTOs.add(mapEntityToResponseDto(existingProduct));
                    }
                } else {
                    ProductResponseDTO createdProduct = createProduct(dto);
                    responseDTOs.add(createdProduct);
                }
            } catch (Exception e) {
                logger.error("Failed to process product {}: {}", dto.getName(), e.getMessage());
            }
        }
        
        return responseDTOs;
    }

    private boolean isProductModified(Product existingProduct, ProductDTO dto) {
        return !existingProduct.getName().equals(dto.getName()) ||
               !existingProduct.getDescription().equals(dto.getDescription()) ||
               existingProduct.getPrice().compareTo(dto.getPrice()) != 0 ||
               !existingProduct.getCategory().getId().equals(dto.getCategoryId()) ||
               !existingProduct.getSubCategory().equals(dto.getSubCategory()) ||
               !existingProduct.getQuantity().equals(dto.getQuantity()) ||
               !existingProduct.getQuantityType().equals(dto.getQuantityType()) ||
               !existingProduct.getStatus().equals(dto.getStatus()) ||
               (existingProduct.getExpiryDate() == null && dto.getExpiryDate() != null) ||
               (existingProduct.getExpiryDate() != null && !existingProduct.getExpiryDate().equals(dto.getExpiryDate()));
    }

    private Product updateProductFromDTO(Product existingProduct, ProductDTO dto) {
        existingProduct.setName(dto.getName());
        existingProduct.setDescription(dto.getDescription());
        existingProduct.setPrice(dto.getPrice());
        
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        existingProduct.setCategory(category);
        existingProduct.setSubCategory(dto.getSubCategory());
        
        existingProduct.setQuantity(dto.getQuantity());
        existingProduct.setQuantityType(dto.getQuantityType());
        existingProduct.setStatus(dto.getStatus());
        existingProduct.setExpiryDate(dto.getExpiryDate());
        
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            deleteProductImages(existingProduct);
            List<String> imageUrls = handleImageUploads(dto.getImages(), existingProduct);
            updateProductImages(existingProduct, imageUrls);
        }
        
        return existingProduct;
    }

    private String normalizePath(String path) {
        return path.replaceFirst("^.*/", "");
    }

    private String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }  

    private String getStringCellValue(Row row, int cellNum) {
        Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? null : cell.getStringCellValue();
    }

    private BigDecimal parseBigDecimalCell(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            throw new IllegalArgumentException("Missing required numeric value at column " + (cellIndex + 1));
        }
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING:
                try {
                    return new BigDecimal(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid numeric value: " + cell.getStringCellValue());
                }
            default:
                throw new IllegalArgumentException("Unsupported cell type for numeric value");
        }
    }

    private Integer parseIntegerCell(Row row, int cellIndex) {
        return parseBigDecimalCell(row, cellIndex).intValue();
    }

    private Long parseLongCell(Row row, int cellIndex) {
        return parseBigDecimalCell(row, cellIndex).longValue();
    }

    private LocalDate parseDateCell(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return null;
        }
        
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                return LocalDate.parse(cell.getStringCellValue().trim());
            }
        } catch (Exception e) {
            logger.warn("Invalid date format in row {} column {}", row.getRowNum() + 1, cellIndex + 1);
        }
        return null;
    }

    private static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;
        
        public InMemoryMultipartFile(String name, String originalFilename, 
                                   String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }
        
        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content == null || content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), content);
        }
    }
}