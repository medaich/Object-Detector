package com.example.objectdetector.data

import com.example.objectdetector.R
import com.example.objectdetector.model.ApiType

object DataSource {
    val apiList = listOf(
        ApiType(
            label = R.string.rest_api,
            description = R.string.rest_api_description,
            icon = R.drawable.rest_api_icon
        ),
        ApiType(
            label = R.string.grpc_api,
            description = R.string.grpc_api_description,
            icon = R.drawable.grpc_logo
        )
    )

    val uiSteps = listOf(
        R.string.select_image,
        R.string.select_api,
        R.string.start_detection
    )

    val cocoLabels = listOf(
                R.string.person,
                R.string.bicycle,
                R.string.car
                ,R.string.motorcycle
                ,R.string.airplane
                ,R.string.bus
                ,R.string.train
                ,R.string.truck
                ,R.string.boat
                ,R.string.traffic_light
                ,R.string.fire_hydrant
                ,R.string.street_sign
                ,R.string.stop_sign
                ,R.string.parking_meter
                ,R.string.bench
                ,R.string.bird
                ,R.string.cat
                ,R.string.dog
                ,R.string.horse
                ,R.string.sheep
                ,R.string.cow
                ,R.string.elephant
                ,R.string.bear
                ,R.string.zebra
                ,R.string.giraffe
                ,R.string.hat
                ,R.string.backpack
                ,R.string.umbrella
                ,R.string.shoe
                ,R.string.eye_glasses
                ,R.string.handbag
                ,R.string.tie
                ,R.string.suitcase
                ,R.string.frisbee
                ,R.string.skis
                ,R.string.snowboard
                ,R.string.sports_ball
                ,R.string.kite
                ,R.string.baseball_bat
                ,R.string.baseball_glove
                ,R.string.skateboard
                ,R.string.surfboard
                ,R.string.tennis_racket
                ,R.string.bottle
                ,R.string.plate
                ,R.string.wine_glass
                ,R.string.cup
                ,R.string.fork
                ,R.string.knife
                ,R.string.spoon
                ,R.string.bowl
                ,R.string.banana
                ,R.string.apple
                ,R.string.sandwich
                ,R.string.orange
                ,R.string.broccoli
                ,R.string.carrot
                ,R.string.hot_dog
                ,R.string.pizza
                ,R.string.donut
                ,R.string.cake
                ,R.string.chair
                ,R.string.couch
                ,R.string.potted_plant
                ,R.string.bed
                ,R.string.mirror
                ,R.string.dining_table
                ,R.string.window
                ,R.string.desk
                ,R.string.toilet
                ,R.string.door
                ,R.string.tv
                ,R.string.laptop
                ,R.string.mouse
                ,R.string.remote
                ,R.string.keyboard
                ,R.string.cell_phone
                ,R.string.microwave
                ,R.string.oven
                ,R.string.toaster
                ,R.string.sink
                ,R.string.refrigerator
                ,R.string.blender
                ,R.string.book
                ,R.string.clock
                ,R.string.vase
                ,R.string.scissors
                ,R.string.teddy_bear
                ,R.string.hair_drier
                ,R.string.toothbrush
    )

}