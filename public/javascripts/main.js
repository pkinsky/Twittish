//dynamic navbar offset

$(window).resize(function () {
   $('body').css('padding-top', parseInt($('#main-navbar').css("height"))+10);
});

$(window).load(function () {
   $('body').css('padding-top', parseInt($('#main-navbar').css("height"))+10);
});


var app = angular.module('app', []);

app.factory('ChatService', function() {

   console.log("spinning up ChatService")

  var service = {};

  service.connect = function() {
    if(service.ws) { return; }

    var ws = new WebSocket("ws://localhost:9000/websocket/");

    ws.onopen = function() {
        console.log("ack websocket");
      service.ws.send(JSON.stringify("ACK"));
    };

    ws.onerror = function() {
      alert("error, failed to open connection");
    }

    ws.onmessage = function(message) {
      service.callback(message.data);
    };

    service.ws = ws;
  }

  service.send = function(message) {
    service.ws.send(JSON.stringify(message));
  }

  service.subscribe = function(callback) {
    service.callback = callback;
  }

  return service;
});


function AppCtrl($scope, ChatService) {
  ChatService.connect();

  //map of user_id => object describing public components of user
  $scope.init = function (current_user, user_info) {
    $scope.current_user = current_user;
    $scope.users = user_info;
  }

  //init to null (binding in init)
  $scope.users = null;
  $scope.current_user = null;

  $scope.signup_complete = function () {
    return $scope.users[$scope.current_user].alias.length != 0;
  };


  $scope.messages = [];


  $scope.set_alias = function() {
    var alias_in = $("#alias").val();
    console.log("setting alias: " + alias_in);
    ChatService.send( {user_id:$scope.user_id, alias:alias_in} );
  };


  $scope.push_message = function(post_id, favorite, msg) {
                msg['post_id'] = post_id;
                msg['favorite'] = favorite;
                $scope.messages.unshift(msg);

                  if (!$scope.users.hasOwnProperty(msg.user_id)){
                      console.log("request info for " + msg.user_id);
                      ChatService.send( {user_id: msg.user_id} );
                  }
  }


  $scope.delete_message = function(message) {
    ChatService.send( {delete_message:message.post_id} );
    //remove msg from map? yeah why not bad removal only reflected client side anyway.
    //todo: wait on confirmation

  }

  $scope.favorite_message = function(message) {
    console.log("favorite message: " + JSON.stringify(message))

    var post_id = message.post_id;

    if (message.favorite) {
        message.favorite = false;
        if (!$scope.$$phase) $scope.$apply();
        ChatService.send( {unfavorite_message:message.post_id} );
    }else{
        message.favorite = true;
        if (!$scope.$$phase) $scope.$apply();
        ChatService.send( {favorite_message:message.post_id} );
    }

  }


  ChatService.subscribe(function(message) {
            console.log("msg: " + message);
            var actual = jQuery.parseJSON(message)

            if ('msg' in actual){
                var messages = actual['msg'];



                messages.forEach(function(msg_info) {
                    var post_id = msg_info.post_id;
                    var favorite = msg_info.favorite;
                    var msg = msg_info.msg;
                    console.log("pushing message for info " + msg_info)
                    $scope.push_message(post_id, favorite, msg);
                });



                $scope.messages.sort(function(a,b){
                  return a.timestamp < b.timestamp ? 1 : -1;
                });
            }


            if ('user_info' in actual){
                //console.log("update!");
                var user_info = actual['user_info'];
                $scope.users[user_info.user_id] = {'alias': user_info.alias, 'avatar_url': user_info.avatar_url};
            }

            if ('alias_result' in actual){
                if (actual['alias_result']['pass']){
                    $scope.users[$scope.current_user].alias = actual['alias_result']['alias'];
                } else {
                    alert("alias taken: " +  actual['alias_result']['alias']);
                }
            }

            $scope.$apply();
      }
  );

  $scope.connect = function() {
    ChatService.connect();
  };

  $scope.send = function() {
    var text = $("#tweeter").val();
	if (text.length > 0){
		ChatService.send( {msg:text} );
		$("#tweeter").val("");
	}
  };

}