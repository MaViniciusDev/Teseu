# 🎮 Teseu - Pacman com Agente Inteligente

Um jogo estilo Pacman desenvolvido em Java com um agente autônomo que usa **busca em grafos** para explorar labirintos, coletar comidas e encontrar a saída.

## 📋 Descrição do Projeto

Este projeto implementa um **agente inteligente** que navega em um labirinto usando apenas informações locais de um sensor 3x3. O agente utiliza algoritmos de busca (A*) para:

- 🔍 **Explorar** o labirinto desconhecido
- 🍎 **Coletar** todas as comidas
- 🚪 **Encontrar** e sair pela saída usando o caminho mais curto

### Características Principais

- **Visão limitada:** Agente usa sensor 3x3 (conhece apenas células adjacentes)
- **Mapa dinâmico:** Constrói conhecimento do labirinto gradualmente
- **Algoritmo A*:** Pathfinding otimizado com heurística Manhattan
- **Exploração por fronteiras:** Estratégia gulosa para descobrir áreas desconhecidas
- **Sistema de vidas:** Perde vida ao tentar sair sem coletar todas as comidas
- **Pontuação:** +10 por comida, -1 por passo, +100 ao vencer

## 🏗️ Estrutura do Projeto

```
Teseu/
├── src/
│   ├── Pacman.java          # Classe principal (JFrame)
│   ├── Model.java           # Lógica do jogo e renderização
│   ├── Agent.java           # Agente inteligente (IA)
│   ├── Map.java             # Carregador de labirinto
│   ├── images/              # Sprites do jogo
│   │   ├── down.gif
│   │   ├── up.gif
│   │   ├── left.gif
│   │   ├── right.gif
│   │   ├── food.png
│   │   └── heart.png
│   └── resources/
│       └── maze.txt         # Arquivo do labirinto

```

## 🎯 Como o Agente Funciona

### 1. Sensor
O agente recebe uma matriz 3×3 com informações locais:
- `X` = Parede
- `o` = Comida
- `_` = Corredor vazio
- `E` = Entrada
- `S` = Saída
- `?` = Desconhecido

### 2. Hierarquia de Decisão
```
1. Se coletou as comidas → A* até a saída
2. Se vê comida conhecida → A* até comida mais próxima
3. Senão → Explorar fronteira mais próxima
```

### 3. Exploração
Uma célula é **fronteira** quando:
- É caminhável e conhecida
- Tem pelo menos 1 vizinho desconhecido ('?')
- Pode mover fisicamente para esse vizinho

**Estratégia:** Sempre vai para a fronteira mais próxima (distância Manhattan).

## 🚀 Como Executar

### Pré-requisitos
- **Java JDK 8+** instalado
- **JRE** configurado no PATH

### Opção 1: Compilar e Executar via Terminal

```bash
# 1. Navegar até o diretório do projeto
cd C:\Users\marce\IdeaProjects\Teseu

# 2. Criar diretório de saída (se não existir)
mkdir out

# 3. Compilar todos os arquivos Java
javac -d out src\*.java

# 4. Executar o jogo
java -cp out Pacman
```

### Opção 2: Executar via IntelliJ IDEA

1. Abrir o projeto no IntelliJ
2. Localizar `Pacman.java`
3. Clicar com botão direito → **Run 'Pacman.main()'**

### Opção 3: Via CMD (Windows)

```cmd
cd C:\Users\marce\IdeaProjects\Teseu
javac -d out src\*.java
java -cp out Pacman
```

## 🎮 Controles

### Durante o Jogo
- **ESPAÇO** - Iniciar/Reiniciar jogo
- **A** - Ligar/Desligar IA (modo manual)
- **↑ ↓ ← →** - Controlar manualmente (quando IA desligada)
- **ESC** - Voltar para tela inicial

### Modos
- **IA Ligada (padrão):** Agente explora automaticamente
- **IA Desligada:** Controle manual com setas

## 📝 Formato do Labirinto de exemplo (maze.txt)

```
XXXXXXXXXXXXX
XE__________X    E = Posição inicial (Entrada)
X_XXXXX_XXX_X    o = Comida
X_____X_____X    S = Saída
X_o_____o___X    X = Parede
X___________X    _ = Corredor
X______S____X
XXXXXXXXXXXXX
```

### Regras do Arquivo
- Primeira e última linha/coluna devem ser paredes (`X`)
- Exatamente 1 entrada (`E`)
- Exatamente 1 saída (`S`)
- 4 comidas (`o`) recomendado
- Espaços vazios representados por `_`

## 📊 Sistema de Pontuação

| Ação | Pontos |
|------|--------|
| Coletar comida | +10 |
| Dar um passo | -1 |
| Vencer (sair) | +100 |
| Tentar sair sem coletar tudo | Perde 1 vida |

## 🧠 Algoritmos Implementados

### A* (A-Star)
- **Heurística:** Distância Manhattan
- **Custo:** Número de passos
- **Uso:** Calcular caminho mais curto até comida, saída ou fronteira

### Busca em Largura (BFS) - Conceitual
A exploração por fronteiras funciona como um BFS implícito, sempre expandindo para células não visitadas mais próximas.

## 🔧 Tecnologias Utilizadas

- **Java SE** (Swing para GUI)
- **Collections Framework** (PriorityQueue, HashMap, HashSet)
- **Graphics2D** para renderização

## 📦 Classes Principais

### `Pacman.java`
JFrame principal que inicializa a janela do jogo.

### `Model.java`
- Gerencia estado do jogo (vidas, pontos, comidas)
- Renderiza labirinto, Pacman e HUD
- Controla física de movimento (paredes, colisões)
- Fornece API para o agente (sensor, validações)

### `Agent.java`
- Constrói conhecimento do mapa via sensor
- Implementa A* para pathfinding
- Decide ações baseado em prioridades
- Explora fronteiras desconhecidas

### `Map.java`
Utilitário para carregar labirinto de arquivo texto.

## 🐛 Troubleshooting

### Erro: "Could not find or load main class Pacman"
**Solução:** Certifique-se de estar na pasta correta e usar `-cp out`
```bash
java -cp out Pacman
```

### Imagens não aparecem
**Solução:** Verifique se a pasta `src/images/` existe e contém todos os arquivos .gif/.png

### Agente fica parado
**Solução:** 
- Pressione **A** para verificar se IA está ligada
- Reinicie com **ESPAÇO**
- Verifique console para logs de debug

## 📈 Melhorias Futuras

- [ ] Adicionar fantasmas (inimigos)
- [ ] Power-ups (aumentar velocidade, invencibilidade)
- [ ] Múltiplos níveis de dificuldade
- [ ] Highscore persistente
- [ ] Visualização do caminho planejado
- [ ] Algoritmo de exploração mais sofisticado (DFS, Dijkstra)

## 👤 Autor

Feitor por mim MaViniciusDev :P

Projeto desenvolvido como Atividade de **Inteligência Artificial** da UCSAL.

## 📄 Licença

Projeto educacional - uso livre para fins acadêmicos.



