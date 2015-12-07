$(function () {
    var nextTimeout;

    newSlider('Order');
    newGauge('#dojrzewatr', 'Maturing', 'present/maturing');
    newGauge('#butelkatr', 'Bottling', 'present/bottling');
    newAggrgtrMetrics('#aggregator', 'Aggregation');

    $('#order').on('click', function () {
        var $btn = $(this).button('loading')
        order();
        setTimeout(function() {
            $btn.button('reset')
        }, 200);
      })

    $('#auto').on('change', function() {
        // always clear the last timeout if any
        if (nextTimeout) {
            clearTimeout(nextTimeout);
        }

        if(this.checked) {
            var timeoutFct = function() {
                nextTimeout = setTimeout(timeoutFct, getAutoInterval());
                $('#order').click();
            };

            nextTimeout = setTimeout(timeoutFct, getAutoInterval());
        }
    });

    setInterval(function(){
        $.get("present/bottles", function(data) {
            $("#bottles").text("Got: "+data+" bottles");
        });
    }, 1000);
});

function getAutoInterval() {
    return (60 / ($('#sliderLinkOrder').text())) * 1000;
}

function order() {
    $.ajax({
          url:'present/order',
          type:"POST",
          data:JSON.stringify(buildOrderRequest()),
          contentType:"application/json; charset=utf-8",
          dataType:"json",
          success: function(data){
            var ing = data.ingredients;
            for (var i = 0; i < ing.length; i++) {
                var $input = $('#'+ing[i].type);
                $input.val(ing[i].quantity);
                $input.change();
            }
          }
    });
}

function buildOrderRequest() {
    var req = {};
    req.items = new Array();

    var items = new Array();
    if ($("#chkWater").is(':checked')) {
        req.items.push('WATER');
    }
    if ($("#chkHop").is(':checked')) {
        req.items.push('HOP');
    }
    if ($("#chkYeast").is(':checked')) {
        req.items.push('YEAST');
    }
    if ($("#chkMalt").is(':checked')) {
        req.items.push('MALT');
    }
    return req;
}

function newSlider(name) {
    new dhtmlXSlider({
				parent: 'sliderObj' + name,
				linkTo: 'sliderLink' + name,
				step: 1,
				min: 10,
				max: 720,
				value: 60
			});
}

function newAggrgtrMetrics(element, name) {

            var brandsData = [["Malt", 0], ["Water", 0], ["Hop", 0], ["Yeast", 0]];


            // Create the chart
            $(element).highcharts({
                chart: {
                    type: 'column'
                },
                title: {
                    text: name
                },
                xAxis: {
                    type: 'category'
                },
                yAxis: {
                    title: {
                        text: 'Stock'
                    }
                },
                legend: {
                    enabled: false
                },
                plotOptions: {
                    series: {
                        borderWidth: 0,
                        dataLabels: {
                            enabled: true,
                            format: '{point.y}'
                        }
                    }
                },

                tooltip: {
                    headerFormat: '<span style="font-size:11px">{series.name}</span><br>',
                    pointFormat: '<span style="color:{point.color}">{point.name}</span>: <b>{point.y}</b> items<br/>'
                },

                series: [{
                    name: 'Stock',
                    colorByPoint: true,
                    data: brandsData
                }]
            },
            function (chart) {
                $('#MALT').change(function(input){
                    chart.series[0].points[0].update(parseInt(input.target.value));
                });
                $('#WATER').change(function(input){
                    chart.series[0].points[1].update(parseInt(input.target.value));
                });
                $('#HOP').change(function(input){
                    chart.series[0].points[2].update(parseInt(input.target.value));
                });
                $('#YEAST').change(function(input){
                    chart.series[0].points[3].update(parseInt(input.target.value));
                });
            });
}

function newGauge(id, name, valueUri) {
    $(id).highcharts({

            chart: {
                type: 'gauge',
                alignTicks: false,
                plotBackgroundColor: null,
                plotBackgroundImage: null,
                plotBorderWidth: 0,
                plotShadow: false
            },

            title: {
                text: name
            },

            pane: {
                startAngle: -150,
                endAngle: 150
            },

            yAxis: [{
                min: 0,
                max: 200,
                tickPosition: 'outside',
                minorTickPosition: 'outside',
                lineColor: '#339',
                tickColor: '#339',
                minorTickColor: '#339',
                offset: -25,
                lineWidth: 2,
                labels: {
                    distance: -20,
                    rotation: 'auto'
                },
                tickLength: 5,
                minorTickLength: 5,
                endOnTick: false
            }],

            series: [{
                name: 'Amount',
                data: [0],
                dataLabels: {
                    formatter: function () {
                        var amount = this.y;
                        return '<span style="color:#339">' + amount + ' items</span><br/>';
                    },
                    backgroundColor: {
                        linearGradient: {
                            x1: 0,
                            y1: 0,
                            x2: 0,
                            y2: 1
                        },
                        stops: [
                            [0, '#DDD'],
                            [1, '#FFF']
                        ]
                    }
                },
                tooltip: {
                    valueSuffix: ' items'
                }
            }]

        },
            // Add some life
            function (chart) {
                setInterval(function () {
                    $.get(valueUri, function(data) {
                        chart.series[0].points[0].update(parseInt(data));
                    });
                }, 1000);

            });
}