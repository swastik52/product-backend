package com.codelife.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductApiTests {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository repository;

    @BeforeEach
    void clearProducts() {
        repository.deleteAll();
    }

    @Test
    void createThenListAndFetchProduct() throws Exception {
        String location = mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Keyboard","description":"USB keyboard","price":49.99}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("USB keyboard"))
                .andExpect(jsonPath("$.price").value(49.99));
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Keyboard"));
    }

    @Test
    void emptyListReturnsEmptyArray() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void missingProductReturns404() throws Exception {
        mvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidProductRequestsReturn400WithoutSaving() throws Exception {
        for (String body : new String[] {
                "{\"name\":\" \",\"price\":10}",
                "{\"name\":\"Keyboard\",\"price\":-1}",
                "{\"name\":\"Keyboard\"}",
                "{\"name\":\"Keyboard\",\"price\":1.234}",
                "{\"name\":\"Keyboard\",\"price\":10000000000}",
                "{\"name\":\"" + "x".repeat(151) + "\",\"price\":10}",
                "{\"name\":\"Keyboard\",\"description\":\"" + "x".repeat(2001) + "\",\"price\":10}",
                "{invalid json}"
        }) {
            mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/products")).andExpect(content().json("[]"));
    }

    @Test
    void invalidIdReturns400() throws Exception {
        for (String id : new String[] {"0", "-1", "abc"}) {
            mvc.perform(get("/api/products/" + id)).andExpect(status().isBadRequest());
        }
    }
}
