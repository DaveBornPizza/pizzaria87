-- Funcionários iniciais (Admin William, Garçom Mike, Cozinheiro Jeremy)
MERGE INTO funcionario (identificador, nome, cargo, pin_acesso, ativo) KEY (identificador) VALUES
    ('william', 'William', 'ADMINISTRADOR', '1234', true),
    ('mike01', 'Mike S.', 'GARCOM', '1111', true),
    ('jeremy02', 'Jeremy F.', 'COZINHEIRO', '2222', true);

-- Itens iniciais do cardápio
MERGE INTO item_cardapio (nome, descricao, categoria, preco, disponivel) KEY (nome) VALUES
    ('Pizza de Batata Frita', 'Pizza com batata frita', 'Pizzas', 54.90, true),
    ('Pizza Quatro Queijos', 'Quatro queijos', 'Pizzas', 46.00, false),
    ('Refrigerante', 'Bebida gelada', 'Bebidas', 9.00, true),
    ('Cupcake de morango', 'Sobremesa com cobertura de morango', 'Sobremesas', 12.00, true);

-- Fichas iniciais (1 a 6)
MERGE INTO ficha (numero_ficha, status) KEY (numero_ficha) VALUES
    (1, 'LIVRE'),
    (2, 'LIVRE'),
    (3, 'LIVRE'),
    (4, 'LIVRE'),
    (5, 'DESATIVADA'),
    (6, 'LIVRE');