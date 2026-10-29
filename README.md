# Slashr

## O Slashr é um bot no telegram que avisa quando um jogo entra em promoçao na Steam.
Voce pode acompanhar um jogo, de tempos em tempos, consultar os preços no IsThereAnyDeal (ITAD) e, se o jogo estiver em promoçao
manda uma mensagem marcando a pessoa dizendo para ela se vale a pena comprar ou nao.

![Slashr em ação](docs/slashr-demo.gif)


## Explicaçao Pasta a Pasta

**Bot** 
- TelegramBot: é o responsavel por receber as mensagens do usuario
- TecladoEscolhaJogo: Monta os botões de escolha do jogo ao usuario
- CallbackAdd: Define e valida o texto escondido dentro de cada botao, ou seja, envia a escolha do usuario.
- CacheBuscas: Guarda na memoria, durante 10 minutos, os resultados da busca.

**Collector**
- PriceCollector: é a estrutura de onde vem os preços com desconto.
- ItadPriceCollector: Faz as chamadas para a API da ITAD
- GameMatch: Resultado da busca do Id e nome do jogo.
- 